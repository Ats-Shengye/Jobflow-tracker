package com.jobflow.tracker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import com.jobflow.tracker.controller.ApiTestSupport.LoggedInUser;
import com.jobflow.tracker.entity.ApplicationStatus;

import tools.jackson.databind.json.JsonMapper;

// 応募登録・更新の入力チェック（必須・文字数・面接回数・選択肢のコードと日付の形）を、API を通して確かめる。
// 求職者は毎回新しく登録して使う（テスト用の DB を他のテストクラスと共有しているため）。
@SpringBootTest
class JobApplicationValidationApiTests {

	private static final String INVALID_INPUT = "入力内容を確認してください";

	private static final String INTERVIEW_ROUND_INVALID = "面接回数は1〜5で選択してください";

	// 全角スペース（U+3000）。前後の空白として削られることを確かめるのに使う。
	private static final String FULL_WIDTH_SPACE = String.valueOf((char) 0x3000);

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = ApiTestSupport.mockMvc(context);
	}

	// ---- 面接回数 ----

	// 面接以外のステータスで送られた面接回数は、エラーにせず空で保存する。
	@ParameterizedTest
	@EnumSource(value = ApplicationStatus.class, names = "INTERVIEW", mode = EnumSource.Mode.EXCLUDE)
	void create_interviewRoundWithNonInterviewStatus_isSavedAsNull(ApplicationStatus status) throws Exception {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("status", status.name());
		body.put("interview_round", 3);

		String json = post(newSeeker().session(), body)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value(status.name()))
				.andExpect(jsonPath("$.interview_round").value(nullValue()))
				.andReturn().getResponse().getContentAsString();

		Integer stored = jdbcTemplate.queryForObject(
				"select interview_round from applications where application_id = ?", Integer.class,
				ApiTestSupport.applicationIdOf(json));
		assertThat(stored).isNull();
	}

	// 1（一次面接）と 5 が範囲の両端。
	@ParameterizedTest
	@ValueSource(ints = { 1, 5 })
	void create_interviewWithRoundAtBoundary_isAccepted(int round) throws Exception {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("interview_round", round);

		post(newSeeker().session(), body)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.interview_round").value(round));
	}

	// 面接の時は必須で 1〜5。null・範囲のすぐ外（0 と 6）・負の数は 400。
	@ParameterizedTest
	@NullSource
	@ValueSource(ints = { 0, 6, -1 })
	void create_interviewWithRoundMissingOrOutOfRange_returns400(Integer round) throws Exception {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("interview_round", round);

		post(newSeeker().session(), body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(INVALID_INPUT))
				.andExpect(jsonPath("$.errors.interview_round").value(INTERVIEW_ROUND_INVALID));
	}

	// キーそのものが無い時も、null と同じく未入力として扱う。
	@Test
	void create_interviewWithoutRoundKey_returns400() throws Exception {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.remove("interview_round");

		post(newSeeker().session(), body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.interview_round").value(INTERVIEW_ROUND_INVALID));
	}

	// 更新でも同じチェックをし、エラーの時は元の応募を変えない。
	@Test
	void update_invalidInput_returns400AndKeepsApplication() throws Exception {
		LoggedInUser seeker = newSeeker();
		long id = ApiTestSupport.applicationIdOf(ApiTestSupport.createApplication(mockMvc, jsonMapper,
				seeker.session(), ApiTestSupport.sampleApplicationBody()));
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("company_name", "");
		body.put("interview_round", 6);

		ApiTestSupport.putApplication(mockMvc, seeker.session(), id, jsonMapper.writeValueAsString(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.company_name").value("企業名を入力してください"))
				.andExpect(jsonPath("$.errors.interview_round").value(INTERVIEW_ROUND_INVALID));

		mockMvc.perform(get("/api/applications/{id}", id).session(seeker.session()))
				.andExpect(jsonPath("$.company_name").value(ApiTestSupport.SAMPLE_COMPANY_NAME))
				.andExpect(jsonPath("$.interview_round").value(2));
	}

	// ---- 必須・文字数 ----

	// 必須の5項目が無ければ、項目ごとにエラーを返す。ステータスが無い時は面接回数のエラーは出さない。
	@Test
	void create_emptyBody_returns400ForEachRequiredField() throws Exception {
		ApiTestSupport.postApplication(mockMvc, newSeeker().session(), "{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(INVALID_INPUT))
				.andExpect(jsonPath("$.errors.company_name").value("企業名を入力してください"))
				.andExpect(jsonPath("$.errors.position").value("ポジションを入力してください"))
				.andExpect(jsonPath("$.errors.employment_insurance").value("雇用保険の有無を入力してください"))
				.andExpect(jsonPath("$.errors.applied_date").value("申込日を入力してください"))
				.andExpect(jsonPath("$.errors.status").value("進捗ステータスを入力してください"))
				.andExpect(jsonPath("$.errors.interview_round").doesNotExist())
				.andExpect(jsonPath("$.errors.salary").doesNotExist())
				.andExpect(jsonPath("$.errors.memo").doesNotExist());
	}

	static Stream<String> blankValues() {
		return Stream.of("", "   ", FULL_WIDTH_SPACE.repeat(2));
	}

	// 空白だけ（全角スペースを含む）は、前後を削ると空になるので未入力として扱う。
	@ParameterizedTest
	@MethodSource("blankValues")
	void create_blankCompanyNameAndPosition_returns400AsRequired(String blank) throws Exception {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("company_name", blank);
		body.put("position", blank);

		post(newSeeker().session(), body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.company_name").value("企業名を入力してください"))
				.andExpect(jsonPath("$.errors.position").value("ポジションを入力してください"));
	}

	// 上限ちょうどは通る。前後の空白は削ってから数えるので、上限の文字数に空白が付いていても通る。
	@Test
	void create_textFieldsAtMaxLength_areAccepted() throws Exception {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("company_name", " " + "社".repeat(100) + FULL_WIDTH_SPACE);
		body.put("position", "職".repeat(100));
		body.put("salary", "円".repeat(100));
		body.put("work_location", "地".repeat(100));
		body.put("memo", "メ".repeat(1000));

		post(newSeeker().session(), body)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.company_name").value("社".repeat(100)))
				.andExpect(jsonPath("$.position").value("職".repeat(100)))
				.andExpect(jsonPath("$.salary").value("円".repeat(100)))
				.andExpect(jsonPath("$.work_location").value("地".repeat(100)))
				.andExpect(jsonPath("$.memo").value("メ".repeat(1000)));
	}

	static Stream<Arguments> tooLongFields() {
		return Stream.of(
				Arguments.of("company_name", "社".repeat(101), "企業名は100文字以内で入力してください"),
				Arguments.of("position", "職".repeat(101), "ポジションは100文字以内で入力してください"),
				Arguments.of("salary", "円".repeat(101), "給与は100文字以内で入力してください"),
				Arguments.of("work_location", "地".repeat(101), "勤務地は100文字以内で入力してください"),
				Arguments.of("memo", "メ".repeat(1001), "メモは1000文字以内で入力してください"));
	}

	// 上限のすぐ上（101 文字・1001 文字）は 400。
	@ParameterizedTest
	@MethodSource("tooLongFields")
	void create_textFieldOverMaxLength_returns400(String field, String value, String expectedMessage)
			throws Exception {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put(field, value);

		post(newSeeker().session(), body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors." + field).value(expectedMessage));
	}

	// 任意項目は、空文字・空白だけ・改行だけなら null として保存する。
	@Test
	void create_blankOptionalFields_areSavedAsNull() throws Exception {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("salary", "");
		body.put("work_location", "   ");
		body.put("memo", FULL_WIDTH_SPACE + "\n");

		String json = post(newSeeker().session(), body)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.salary").value(nullValue()))
				.andExpect(jsonPath("$.work_location").value(nullValue()))
				.andExpect(jsonPath("$.memo").value(nullValue()))
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select salary, work_location, memo from applications where application_id = ?",
				ApiTestSupport.applicationIdOf(json));
		assertThat(row.values()).containsOnlyNulls();
	}

	// ---- 選択肢のコードと日付の形 ----

	static Stream<Arguments> malformedValues() {
		return Stream.of(
				Arguments.of("status", "BOGUS"), // 決めていないコード
				Arguments.of("status", "interview"), // 小文字
				Arguments.of("status", 2), // 番号（Jackson の既定では INTERVIEW に読み替えられる）
				Arguments.of("status", "2"), // 番号の文字列
				Arguments.of("employment_insurance", "MAYBE"),
				Arguments.of("applied_date", "2026/10/30"), // 区切りが違う
				Arguments.of("applied_date", "2026-02-30"), // 存在しない日
				Arguments.of("applied_date", "2026-10-30T10:00"), // 時刻付き（既定では時刻を捨てて通る）
				Arguments.of("applied_date", 20261030), // 数値（既定ではエポックからの日数として読まれる）
				Arguments.of("interview_round", 2.5), // 小数（既定では 2 に切り捨てられる）
				Arguments.of("interview_round", "abc"));
	}

	// 選択肢のコードや日付の形が違う時は、項目ごとのエラーではなく形式違いの 400。
	@ParameterizedTest
	@MethodSource("malformedValues")
	void create_malformedValue_returns400(String field, Object value) throws Exception {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put(field, value);

		post(newSeeker().session(), body)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("リクエストの形式が正しくありません"))
				.andExpect(jsonPath("$.errors").doesNotExist());
	}

	private LoggedInUser newSeeker() throws Exception {
		return ApiTestSupport.registerAndLoginSeeker(mockMvc, jsonMapper, "入力 試験");
	}

	private ResultActions post(MockHttpSession session, Map<String, Object> body) throws Exception {
		return ApiTestSupport.postApplication(mockMvc, session, jsonMapper.writeValueAsString(body));
	}
}
