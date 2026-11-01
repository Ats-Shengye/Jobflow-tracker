package com.jobflow.tracker.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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

	private ApiTestSupport() {
	}

	static MockMvc mockMvc(WebApplicationContext context) {
		return MockMvcBuilders.webAppContextSetup(context)
				.apply(SecurityMockMvcConfigurers.springSecurity())
				.defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
				.build();
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
}
