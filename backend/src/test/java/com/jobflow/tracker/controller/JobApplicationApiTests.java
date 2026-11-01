package com.jobflow.tracker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.jobflow.tracker.controller.ApiTestSupport.LoggedInUser;

import tools.jackson.databind.json.JsonMapper;

// 応募の API の正常系・持ち主の確認・役割による拒否・ログを、Security のフィルターから DB まで通して確かめる。
// 入力チェックは JobApplicationValidationApiTests。求職者は毎回新しく登録して使う（テスト用の DB を共有しているため）。
@SpringBootTest
class JobApplicationApiTests {

	private static final String NOT_FOUND = "対象が見つかりません";

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

	// ---- 正常系 ----

	// updated_at は保存した時刻を日本時間に直したもの。形だけでなく、値が今の時刻であることも見る。
	@Test
	void create_validInput_returns201WithAllFields() throws Exception {
		LoggedInUser seeker = newSeeker();
		Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		String json = post(seeker.session(), ApiTestSupport.sampleApplicationBody())
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.application_id").isNumber())
				.andExpect(jsonPath("$.company_name").value(ApiTestSupport.SAMPLE_COMPANY_NAME))
				.andExpect(jsonPath("$.position").value("Webエンジニア"))
				.andExpect(jsonPath("$.employment_insurance").value("YES"))
				.andExpect(jsonPath("$.applied_date").value("2026-10-30"))
				.andExpect(jsonPath("$.status").value("INTERVIEW"))
				.andExpect(jsonPath("$.interview_round").value(2))
				.andExpect(jsonPath("$.salary").value("年収400万円"))
				.andExpect(jsonPath("$.work_location").value("福岡市"))
				.andExpect(jsonPath("$.memo").value(ApiTestSupport.SAMPLE_MEMO))
				.andExpect(jsonPath("$.updated_at").value(matchesPattern(ApiTestSupport.UPDATED_AT_FORMAT)))
				.andExpect(jsonPath("$.user_id").doesNotExist())
				.andReturn().getResponse().getContentAsString();

		Instant updatedAt = OffsetDateTime.parse(JsonPath.read(json, "$.updated_at")).toInstant();
		assertThat(updatedAt).isBetween(before, Instant.now());
		assertThat(ownerOf(ApiTestSupport.applicationIdOf(json))).isEqualTo(seeker.userId());
	}

	// 本文に別人の user_id や updated_at を入れても読まれない。持ち主はログイン中の利用者になる。
	@Test
	void create_withOtherUsersIdInBody_ownerIsLoggedInUser() throws Exception {
		LoggedInUser seeker = newSeeker();
		LoggedInUser other = newSeeker();
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("user_id", other.userId());
		body.put("updated_at", "2000-01-01T00:00:00+09:00");

		String json = ApiTestSupport.createApplication(mockMvc, jsonMapper, seeker.session(), body);

		assertThat(ownerOf(ApiTestSupport.applicationIdOf(json))).isEqualTo(seeker.userId());
		assertThat((String) JsonPath.read(json, "$.updated_at")).doesNotStartWith("2000-");
		mockMvc.perform(get("/api/applications").session(other.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	// 申込日の降順、同じ日は ID の降順。他人の応募は出ず、カード用の項目だけを返す（面接回数は null でもキーを出す）。
	@Test
	void list_returnsOwnApplicationsNewestFirstWithCardFieldsOnly() throws Exception {
		LoggedInUser seeker = newSeeker();
		LoggedInUser other = newSeeker();
		Map<String, Object> appliedOnly = bodyWithDate("2026-10-01");
		appliedOnly.put("status", "APPLIED");
		long oldest = create(seeker, appliedOnly);
		long sameDayFirst = create(seeker, bodyWithDate("2026-10-10"));
		long sameDaySecond = create(seeker, bodyWithDate("2026-10-10"));
		long newest = create(seeker, bodyWithDate("2026-10-20"));
		create(other, bodyWithDate("2026-10-25"));

		String json = mockMvc.perform(get("/api/applications").session(seeker.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].company_name").value(ApiTestSupport.SAMPLE_COMPANY_NAME))
				.andExpect(jsonPath("$[0].position").value("Webエンジニア"))
				.andExpect(jsonPath("$[0].status").value("INTERVIEW"))
				.andExpect(jsonPath("$[0].interview_round").value(2))
				.andExpect(jsonPath("$[0].applied_date").value("2026-10-20"))
				.andExpect(jsonPath("$[0].employment_insurance").doesNotExist())
				.andExpect(jsonPath("$[0].salary").doesNotExist())
				.andExpect(jsonPath("$[0].work_location").doesNotExist())
				.andExpect(jsonPath("$[0].memo").doesNotExist())
				.andExpect(jsonPath("$[0].updated_at").doesNotExist())
				.andExpect(jsonPath("$[3].status").value("APPLIED"))
				.andExpect(jsonPath("$[3].interview_round").value(nullValue()))
				.andReturn().getResponse().getContentAsString();

		List<Number> ids = JsonPath.read(json, "$[*].application_id");
		assertThat(ids).extracting(Number::longValue)
				.containsExactly(newest, sameDaySecond, sameDayFirst, oldest);
	}

	@Test
	void get_ownApplication_returns200WithAllFields() throws Exception {
		LoggedInUser seeker = newSeeker();
		long id = create(seeker, ApiTestSupport.sampleApplicationBody());

		mockMvc.perform(get("/api/applications/{id}", id).session(seeker.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.application_id").value(id))
				.andExpect(jsonPath("$.company_name").value(ApiTestSupport.SAMPLE_COMPANY_NAME))
				.andExpect(jsonPath("$.employment_insurance").value("YES"))
				.andExpect(jsonPath("$.interview_round").value(2))
				.andExpect(jsonPath("$.salary").value("年収400万円"))
				.andExpect(jsonPath("$.work_location").value("福岡市"))
				.andExpect(jsonPath("$.memo").value(ApiTestSupport.SAMPLE_MEMO))
				.andExpect(jsonPath("$.updated_at").value(matchesPattern(ApiTestSupport.UPDATED_AT_FORMAT)));
	}

	// 全項目の置き換えなので、送らなかった任意項目は null になる。面接以外に変えると、送った面接回数も空になる。
	@Test
	void update_ownApplication_replacesAllFields() throws Exception {
		LoggedInUser seeker = newSeeker();
		long id = create(seeker, ApiTestSupport.sampleApplicationBody());
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("company_name", "更新後株式会社");
		body.put("position", "社内SE");
		body.put("employment_insurance", "UNKNOWN");
		body.put("applied_date", "2026-11-01");
		body.put("status", "OFFERED");
		body.remove("salary");
		body.remove("work_location");
		body.remove("memo");

		put(seeker.session(), id, body)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.application_id").value(id))
				.andExpect(jsonPath("$.company_name").value("更新後株式会社"))
				.andExpect(jsonPath("$.position").value("社内SE"))
				.andExpect(jsonPath("$.employment_insurance").value("UNKNOWN"))
				.andExpect(jsonPath("$.applied_date").value("2026-11-01"))
				.andExpect(jsonPath("$.status").value("OFFERED"))
				.andExpect(jsonPath("$.interview_round").value(nullValue()))
				.andExpect(jsonPath("$.salary").value(nullValue()))
				.andExpect(jsonPath("$.work_location").value(nullValue()))
				.andExpect(jsonPath("$.memo").value(nullValue()))
				.andExpect(jsonPath("$.updated_at").value(matchesPattern(ApiTestSupport.UPDATED_AT_FORMAT)));

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select company_name, status, interview_round, memo from applications where application_id = ?", id);
		assertThat(row.get("company_name")).isEqualTo("更新後株式会社");
		assertThat(row.get("status")).isEqualTo("OFFERED");
		assertThat(row.get("interview_round")).isNull();
		assertThat(row.get("memo")).isNull();
	}

	@Test
	void delete_ownApplication_returns200AndRemovesIt() throws Exception {
		LoggedInUser seeker = newSeeker();
		long id = create(seeker, ApiTestSupport.sampleApplicationBody());

		mockMvc.perform(delete("/api/applications/{id}", id).session(seeker.session()).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("削除しました"));

		mockMvc.perform(get("/api/applications/{id}", id).session(seeker.session()))
				.andExpect(status().isNotFound());
		assertThat(countRows(id)).isZero();
	}

	// ---- 持ち主の確認（IDOR） ----

	// 他人の応募は詳細・更新・削除のどれも 404 で、本文も存在しない ID の時と同じ（応募があることを知らせない）。
	// 持ち主の応募は書き換わっても消えてもいないこと。
	@Test
	void otherSeeker_getUpdateDelete_returns404AndLeavesApplicationUnchanged() throws Exception {
		LoggedInUser owner = newSeeker();
		LoggedInUser intruder = newSeeker();
		long id = create(owner, ApiTestSupport.sampleApplicationBody());
		Map<String, Object> tampered = ApiTestSupport.sampleApplicationBody();
		tampered.put("company_name", "書き換え株式会社");
		tampered.put("memo", "書き換えたメモ");

		String otherUsersResponse = mockMvc.perform(get("/api/applications/{id}", id).session(intruder.session()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(NOT_FOUND))
				.andReturn().getResponse().getContentAsString();
		put(intruder.session(), id, tampered)
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/applications/{id}", id).session(intruder.session()).with(csrf()))
				.andExpect(status().isNotFound());
		String unknownIdResponse = mockMvc
				.perform(get("/api/applications/{id}", Long.MAX_VALUE).session(intruder.session()))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

		assertThat(otherUsersResponse).isEqualTo(unknownIdResponse);
		mockMvc.perform(get("/api/applications/{id}", id).session(owner.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.company_name").value(ApiTestSupport.SAMPLE_COMPANY_NAME))
				.andExpect(jsonPath("$.memo").value(ApiTestSupport.SAMPLE_MEMO));
		assertThat(ownerOf(id)).isEqualTo(owner.userId());
	}

	// 存在しない ID・負の ID・数値でない ID・Long に収まらない ID は、どのメソッドでも 404（500 にならない）。
	@ParameterizedTest
	@CsvSource({
			"GET, 9223372036854775807", "PUT, 9223372036854775807", "DELETE, 9223372036854775807",
			"GET, -1",
			"GET, abc", "PUT, abc", "DELETE, abc",
			"GET, 1.5",
			"GET, 99999999999999999999" })
	void unknownOrNonNumericId_returns404(String method, String id) throws Exception {
		send(method, "/api/applications/" + id, newSeeker().session())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(NOT_FOUND));
	}

	// ---- 役割と未ログイン ----

	@ParameterizedTest
	@CsvSource({ "GET, /api/applications", "POST, /api/applications", "GET, /api/applications/1",
			"PUT, /api/applications/1", "DELETE, /api/applications/1" })
	void withoutLogin_returns401(String method, String url) throws Exception {
		send(method, url, null)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("ログインしてください"));
	}

	// 担当者は応募を持たず閲覧のみなので、求職者用の API はどれも 403。
	@ParameterizedTest
	@CsvSource({ "GET, /api/applications", "POST, /api/applications", "GET, /api/applications/1",
			"PUT, /api/applications/1", "DELETE, /api/applications/1" })
	void advisor_onApplicationsApi_returns403(String method, String url) throws Exception {
		LoggedInUser advisor = ApiTestSupport.loginAdvisor(mockMvc, jsonMapper);

		send(method, url, advisor.session())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("この操作を行う権限がありません"));
	}

	// ---- ログ ----

	// ログには user_id と application_id だけを出し、企業名やメモの中身は出さない。
	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void createUpdateDelete_logIdsButNotContent(CapturedOutput output) throws Exception {
		LoggedInUser seeker = newSeeker();
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("company_name", "ログ確認社-4b7e");
		body.put("memo", "ログ確認メモ-91d2");
		long id = create(seeker, body);
		put(seeker.session(), id, body).andExpect(status().isOk());
		mockMvc.perform(delete("/api/applications/{id}", id).session(seeker.session()).with(csrf()))
				.andExpect(status().isOk());

		String ids = "(user_id=" + seeker.userId() + ", application_id=" + id + ")";
		assertThat(output).contains("Created an application " + ids)
				.contains("Updated an application " + ids)
				.contains("Deleted an application " + ids)
				.doesNotContain("ログ確認社-4b7e", "ログ確認メモ-91d2");
	}

	// ---- 道具 ----

	private LoggedInUser newSeeker() throws Exception {
		return ApiTestSupport.registerAndLoginSeeker(mockMvc, jsonMapper, "応募 試験");
	}

	private long create(LoggedInUser seeker, Map<String, Object> body) throws Exception {
		return ApiTestSupport.applicationIdOf(
				ApiTestSupport.createApplication(mockMvc, jsonMapper, seeker.session(), body));
	}

	private static Map<String, Object> bodyWithDate(String appliedDate) {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("applied_date", appliedDate);
		return body;
	}

	private ResultActions post(MockHttpSession session, Map<String, Object> body) throws Exception {
		return ApiTestSupport.postApplication(mockMvc, session, jsonMapper.writeValueAsString(body));
	}

	private ResultActions put(MockHttpSession session, long id, Map<String, Object> body) throws Exception {
		return ApiTestSupport.putApplication(mockMvc, session, id, jsonMapper.writeValueAsString(body));
	}

	// メソッドと URL だけを変えて同じ確認をする。本文は常に正しい応募を付ける（本文の不備で先に弾かれないように）。
	private ResultActions send(String method, String url, MockHttpSession session) throws Exception {
		MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), url).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(ApiTestSupport.sampleApplicationBody()));
		if (session != null) {
			builder.session(session);
		}
		return mockMvc.perform(builder);
	}

	private Long ownerOf(long applicationId) {
		return jdbcTemplate.queryForObject("select user_id from applications where application_id = ?", Long.class,
				applicationId);
	}

	private Integer countRows(long applicationId) {
		return jdbcTemplate.queryForObject("select count(*) from applications where application_id = ?",
				Integer.class, applicationId);
	}
}
