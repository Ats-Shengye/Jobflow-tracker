package com.jobflow.tracker.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.test.web.support.WebTestUtils;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

import tools.jackson.databind.json.JsonMapper;

// API のテストで共通に使う道具。
// @AutoConfigureMockMvc を付けるとコンテキストの設定が変わって別のコンテキストができ、共有しているテスト用 H2 の
// テーブルを create-drop で消し合うので、素の @SpringBootTest のまま MockMvc をここで組み立てる。
final class ApiTestSupport {

	// 初期データ（InitialDataLoader）のデモ用アカウント。
	static final String SEEKER_LOGIN_ID = "seeker01";
	static final String SEEKER_PASSWORD = "seeker-demo-2026";
	static final String ADVISOR_LOGIN_ID = "advisor01";
	static final String ADVISOR_PASSWORD = "advisor-demo-2026";

	// テストで登録する求職者のパスワード（21 文字、半角スペースを含む）。
	static final String TEST_SEEKER_PASSWORD = "correct horse battery";

	static final String SAMPLE_COMPANY_NAME = "テスト株式会社";

	static final String SAMPLE_MEMO = "一次面接は通過。次は技術面接";

	// 応答の updated_at の形（+09:00 付き、秒まで）。
	static final String UPDATED_AT_FORMAT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+09:00";

	private ApiTestSupport() {
	}

	record LoggedInUser(long userId, String name, MockHttpSession session) {
	}

	static MockMvc mockMvc(WebApplicationContext context) {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
				.build();
		// csrf() は CSRF トークンの保存先をテスト用（セッション）に差し替えたまま戻さない。
		// 先に csrf() を使ったテストの後でも Cookie が出るよう、毎回本物の保存先に戻す。
		WebTestUtils.setCsrfTokenRepository(new MockHttpServletRequest(context.getServletContext()),
				context.getBean(CookieCsrfTokenRepository.class));
		return mockMvc;
	}

	static String loginBody(JsonMapper jsonMapper, String loginId, String password) {
		return jsonMapper.writeValueAsString(Map.of("login_id", loginId, "password", password));
	}

	// ログインして、ログイン済みのセッションを返す。
	static MockHttpSession login(MockMvc mockMvc, JsonMapper jsonMapper, String loginId, String password)
			throws Exception {
		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(post("/api/login").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody(jsonMapper, loginId, password)))
				.andExpect(status().isOk());
		return session;
	}

	// テスト用の DB は他のテストクラスと共有なので、応募を書き込むテストは毎回新しい求職者を作って使う。
	// デモの求職者（seeker01）の応募は InitialDataLoaderTests がステータスの揃い方を見ているので触らない。
	static LoggedInUser registerAndLoginSeeker(MockMvc mockMvc, JsonMapper jsonMapper, String name) throws Exception {
		String loginId = "t-" + UUID.randomUUID();
		String body = jsonMapper
				.writeValueAsString(Map.of("login_id", loginId, "password", TEST_SEEKER_PASSWORD, "name", name));
		String response = mockMvc.perform(post("/api/users").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long userId = ((Number) JsonPath.read(response, "$.user_id")).longValue();
		return new LoggedInUser(userId, name, login(mockMvc, jsonMapper, loginId, TEST_SEEKER_PASSWORD));
	}

	// 全項目が入った正しい応募の本文。テストごとに書き換えるので、毎回新しい Map を返す。
	static Map<String, Object> sampleApplicationBody() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("company_name", SAMPLE_COMPANY_NAME);
		body.put("position", "Webエンジニア");
		body.put("employment_insurance", "YES");
		body.put("applied_date", "2026-10-30");
		body.put("status", "INTERVIEW");
		body.put("interview_round", 2);
		body.put("salary", "年収400万円");
		body.put("work_location", "福岡市");
		body.put("memo", SAMPLE_MEMO);
		return body;
	}

	static ResultActions postApplication(MockMvc mockMvc, MockHttpSession session, String json) throws Exception {
		return mockMvc.perform(post("/api/applications").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	static ResultActions putApplication(MockMvc mockMvc, MockHttpSession session, long applicationId, String json)
			throws Exception {
		return mockMvc.perform(put("/api/applications/{id}", applicationId).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	// 応募を登録して、応答の本文（JSON）を返す。
	static String createApplication(MockMvc mockMvc, JsonMapper jsonMapper, MockHttpSession session,
			Map<String, Object> body) throws Exception {
		return postApplication(mockMvc, session, jsonMapper.writeValueAsString(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
	}

	static long applicationIdOf(String responseJson) {
		return ((Number) JsonPath.read(responseJson, "$.application_id")).longValue();
	}

	static LoggedInUser loginAdvisor(MockMvc mockMvc, JsonMapper jsonMapper) throws Exception {
		MockHttpSession session = login(mockMvc, jsonMapper, ADVISOR_LOGIN_ID, ADVISOR_PASSWORD);
		String response = mockMvc.perform(get("/api/users/me").session(session))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		long userId = ((Number) JsonPath.read(response, "$.user_id")).longValue();
		String name = JsonPath.read(response, "$.name");
		return new LoggedInUser(userId, name, session);
	}
}
