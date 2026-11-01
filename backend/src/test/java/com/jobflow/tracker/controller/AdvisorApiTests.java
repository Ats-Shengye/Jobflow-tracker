package com.jobflow.tracker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.jobflow.tracker.controller.ApiTestSupport.LoggedInUser;

import tools.jackson.databind.json.JsonMapper;

// 担当者用の API（求職者一覧・求職者別応募）を、Security のフィルターから DB まで通して確かめる。
// 求職者一覧には他のテストで作った求職者も出るので、確認はこのテストで作った user_id の行に絞る。
@SpringBootTest
class AdvisorApiTests {

	private static final String NOT_FOUND = "対象が見つかりません";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JsonMapper jsonMapper;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = ApiTestSupport.mockMvc(context);
	}

	// 応募2件の求職者と0件の求職者を作る。件数と最終更新日時（最も新しい updated_at）が出て、0件なら null。
	// 担当者は一覧に出ず、並びは user_id の昇順。
	@Test
	void listSeekers_returnsCountAndLastUpdatedPerSeekerWithoutAdvisors() throws Exception {
		LoggedInUser busy = ApiTestSupport.registerAndLoginSeeker(mockMvc, jsonMapper, "一覧 多子");
		LoggedInUser idle = ApiTestSupport.registerAndLoginSeeker(mockMvc, jsonMapper, "一覧 無子");
		createApplication(busy, "2026-10-01");
		String latest = createApplication(busy, "2026-10-02");
		LoggedInUser advisor = ApiTestSupport.loginAdvisor(mockMvc, jsonMapper);

		String json = mockMvc.perform(get("/api/admin/seekers").session(advisor.session()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Map<String, Object> busyRow = rowOf(json, busy.userId());
		String latestUpdatedAt = JsonPath.read(latest, "$.updated_at");
		assertThat(busyRow).containsEntry("name", "一覧 多子").containsEntry("application_count", 2);
		assertThat((String) busyRow.get("last_updated_at")).matches(ApiTestSupport.UPDATED_AT_FORMAT)
				.isEqualTo(latestUpdatedAt);
		assertThat(rowOf(json, idle.userId())).containsEntry("name", "一覧 無子")
				.containsEntry("application_count", 0)
				.containsEntry("last_updated_at", null);
		List<Number> userIds = JsonPath.read(json, "$[*].user_id");
		assertThat(userIds).extracting(Number::longValue).doesNotContain(advisor.userId()).isSorted();
	}

	// 求職者の user_id・氏名と、応募の全項目（給与・勤務地・メモ・最終更新日時を含む）を申込日の降順で返す。
	@Test
	void seekerApplications_returnsSeekerAndAllFieldsNewestFirst() throws Exception {
		LoggedInUser seeker = ApiTestSupport.registerAndLoginSeeker(mockMvc, jsonMapper, "閲覧 対象子");
		Map<String, Object> older = ApiTestSupport.sampleApplicationBody();
		older.put("applied_date", "2026-10-01");
		older.put("status", "APPLIED");
		older.remove("salary");
		long olderId = ApiTestSupport.applicationIdOf(
				ApiTestSupport.createApplication(mockMvc, jsonMapper, seeker.session(), older));
		long newerId = ApiTestSupport.applicationIdOf(createApplication(seeker, "2026-10-20"));
		LoggedInUser advisor = ApiTestSupport.loginAdvisor(mockMvc, jsonMapper);

		mockMvc.perform(get("/api/admin/seekers/{id}/applications", seeker.userId()).session(advisor.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user_id").value(seeker.userId()))
				.andExpect(jsonPath("$.name").value("閲覧 対象子"))
				.andExpect(jsonPath("$.applications.length()").value(2))
				.andExpect(jsonPath("$.applications[0].application_id").value(newerId))
				.andExpect(jsonPath("$.applications[0].company_name").value(ApiTestSupport.SAMPLE_COMPANY_NAME))
				.andExpect(jsonPath("$.applications[0].position").value("Webエンジニア"))
				.andExpect(jsonPath("$.applications[0].employment_insurance").value("YES"))
				.andExpect(jsonPath("$.applications[0].applied_date").value("2026-10-20"))
				.andExpect(jsonPath("$.applications[0].status").value("INTERVIEW"))
				.andExpect(jsonPath("$.applications[0].interview_round").value(2))
				.andExpect(jsonPath("$.applications[0].salary").value("年収400万円"))
				.andExpect(jsonPath("$.applications[0].work_location").value("福岡市"))
				.andExpect(jsonPath("$.applications[0].memo").value(ApiTestSupport.SAMPLE_MEMO))
				.andExpect(jsonPath("$.applications[0].updated_at")
						.value(matchesPattern(ApiTestSupport.UPDATED_AT_FORMAT)))
				.andExpect(jsonPath("$.applications[1].application_id").value(olderId))
				.andExpect(jsonPath("$.applications[1].interview_round").value(nullValue()))
				.andExpect(jsonPath("$.applications[1].salary").value(nullValue()));
	}

	@Test
	void seekerApplications_seekerWithoutApplications_returnsEmptyArray() throws Exception {
		LoggedInUser seeker = ApiTestSupport.registerAndLoginSeeker(mockMvc, jsonMapper, "応募 無し");
		LoggedInUser advisor = ApiTestSupport.loginAdvisor(mockMvc, jsonMapper);

		mockMvc.perform(get("/api/admin/seekers/{id}/applications", seeker.userId()).session(advisor.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user_id").value(seeker.userId()))
				.andExpect(jsonPath("$.applications").isArray())
				.andExpect(jsonPath("$.applications").isEmpty());
	}

	// 存在しない ID・数値でない ID・Long に収まらない ID は 404（500 にならない）。
	@ParameterizedTest
	@ValueSource(strings = { "9223372036854775807", "abc", "1.5", "99999999999999999999" })
	void seekerApplications_unknownOrNonNumericId_returns404(String id) throws Exception {
		LoggedInUser advisor = ApiTestSupport.loginAdvisor(mockMvc, jsonMapper);

		mockMvc.perform(get("/api/admin/seekers/" + id + "/applications").session(advisor.session()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(NOT_FOUND));
	}

	// 担当者のユーザーは求職者ではないので、ID が存在していても 404。
	@Test
	void seekerApplications_advisorUserId_returns404() throws Exception {
		LoggedInUser advisor = ApiTestSupport.loginAdvisor(mockMvc, jsonMapper);

		mockMvc.perform(get("/api/admin/seekers/{id}/applications", advisor.userId()).session(advisor.session()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(NOT_FOUND));
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api/admin/seekers", "/api/admin/seekers/1/applications" })
	void seeker_onAdvisorApi_returns403(String url) throws Exception {
		LoggedInUser seeker = ApiTestSupport.registerAndLoginSeeker(mockMvc, jsonMapper, "権限 確認");

		mockMvc.perform(get(url).session(seeker.session()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("この操作を行う権限がありません"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api/admin/seekers", "/api/admin/seekers/1/applications" })
	void withoutLogin_returns401(String url) throws Exception {
		mockMvc.perform(get(url))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("ログインしてください"));
	}

	// 担当者は閲覧のみ。担当者用の API に書き込みのメソッドは無いので 405 になる。
	@Test
	void advisor_writeMethodsOnAdvisorApi_areNotAllowed() throws Exception {
		LoggedInUser advisor = ApiTestSupport.loginAdvisor(mockMvc, jsonMapper);

		mockMvc.perform(post("/api/admin/seekers").session(advisor.session()).with(csrf()))
				.andExpect(status().isMethodNotAllowed());
		mockMvc.perform(delete("/api/admin/seekers/1/applications").session(advisor.session()).with(csrf()))
				.andExpect(status().isMethodNotAllowed());
	}

	// 申込日だけを変えて応募を登録し、応答の本文を返す。
	private String createApplication(LoggedInUser seeker, String appliedDate) throws Exception {
		Map<String, Object> body = ApiTestSupport.sampleApplicationBody();
		body.put("applied_date", appliedDate);
		return ApiTestSupport.createApplication(mockMvc, jsonMapper, seeker.session(), body);
	}

	// 一覧から指定した user_id の行を1つだけ取り出す。
	private static Map<String, Object> rowOf(String json, long userId) {
		List<Map<String, Object>> rows = JsonPath.read(json, "$[?(@.user_id == " + userId + ")]");
		assertThat(rows).hasSize(1);
		return rows.get(0);
	}
}
