package com.jobflow.tracker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.Role;
import com.jobflow.tracker.repository.AppUserRepository;

import tools.jackson.databind.json.JsonMapper;

// ユーザー登録の API。テスト用の DB は他のテストクラスと共有なので、登録するログイン ID はテストごとに変えている。
@SpringBootTest
class UserRegistrationApiTests {

	// 半角スペースを含む 21 文字のパスフレーズ。文字数・文字種のどちらも満たす。
	private static final String VALID_PASSWORD = "correct horse battery";

	private static final String LOGIN_ID_FORMAT = "ログインIDは半角英数字と記号（空白なし）4〜64文字で入力してください";

	private static final String PASSWORD_FORMAT = "パスワードは半角英数字・記号・半角スペースで15〜64文字で入力してください";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private AppUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = ApiTestSupport.mockMvc(context);
	}

	// 登録だけではログインさせない（詳細設計: 登録後はログイン画面へ）ので、セッションも作られないこと。
	@Test
	void register_validInput_returns201WithoutRoleOrPasswordAndDoesNotLogIn() throws Exception {
		MvcResult result = register("newseeker01", VALID_PASSWORD, "新規 太郎")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.user_id").isNumber())
				.andExpect(jsonPath("$.login_id").value("newseeker01"))
				.andExpect(jsonPath("$.name").value("新規 太郎"))
				.andExpect(jsonPath("$.role").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andReturn();

		assertThat(result.getRequest().getSession(false)).isNull();
		AppUser saved = userRepository.findByLoginId("newseeker01").orElseThrow();
		assertThat(saved.getRole()).isEqualTo(Role.SEEKER);
		assertThat(saved.getPasswordHash()).isNotEqualTo(VALID_PASSWORD);
		assertThat(passwordEncoder.matches(VALID_PASSWORD, saved.getPasswordHash())).isTrue();
	}

	// 本文に role を入れて担当者になろうとしても、求職者として作られる（権限昇格の対策）。
	@Test
	void register_withAdvisorRoleInBody_stillCreatesSeeker() throws Exception {
		Map<String, String> body = registrationBody("rolehack01", VALID_PASSWORD, "権限 試子");
		body.put("role", "ADVISOR");

		postRegistration(jsonMapper.writeValueAsString(body))
				.andExpect(status().isCreated());

		assertThat(userRepository.findByLoginId("rolehack01").orElseThrow().getRole()).isEqualTo(Role.SEEKER);
	}

	@Test
	void register_existingLoginId_returns409() throws Exception {
		register(ApiTestSupport.SEEKER_LOGIN_ID, VALID_PASSWORD, "重複 太郎")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("このログインIDは使用できません"));
	}

	// ログイン ID は大文字小文字を区別するので、seeker01 があっても Seeker01 は別の ID として登録できる。
	@Test
	void register_sameLoginIdWithDifferentCase_isAccepted() throws Exception {
		register("Seeker01", VALID_PASSWORD, "大文字 太郎")
				.andExpect(status().isCreated());
	}

	static Stream<String> acceptedLoginIds() {
		return Stream.of(
				"id04", // 下限ちょうど（4 文字）
				"L".repeat(64), // 上限ちょうど（64 文字）
				"a!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"); // ASCII の記号を全部含む
	}

	@ParameterizedTest
	@MethodSource("acceptedLoginIds")
	void register_loginIdWithinRules_isAccepted(String loginId) throws Exception {
		register(loginId, VALID_PASSWORD, "境界 確認")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.login_id").value(loginId));
	}

	static Stream<String> rejectedLoginIds() {
		return Stream.of(
				"abc", // 下限のすぐ下（3 文字）
				"M".repeat(65), // 上限のすぐ上（65 文字）
				"ａｂｃｄ", // 全角の英字
				"あいうえ", // 日本語
				"ab cd"); // 空白を含む
	}

	@ParameterizedTest
	@MethodSource("rejectedLoginIds")
	void register_loginIdOutsideRules_returns400(String loginId) throws Exception {
		register(loginId, VALID_PASSWORD, "境界 確認")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("入力内容を確認してください"))
				.andExpect(jsonPath("$.errors.login_id").value(LOGIN_ID_FORMAT));
	}

	static Stream<Arguments> acceptedPasswords() {
		return Stream.of(
				Arguments.of("pwmin15", "abcdefghijklmno"), // 下限ちょうど（15 文字）
				Arguments.of("pwmax64", "x".repeat(64)), // 上限ちょうど（64 文字）
				Arguments.of("pwspace01", "correct horse battery staple"), // 単語を空白で並べたパスフレーズ
				Arguments.of("pwedge01", "  edge spaces kept  ")); // 前後の空白も値のうち
	}

	@ParameterizedTest
	@MethodSource("acceptedPasswords")
	void register_passwordWithinRules_isAccepted(String loginId, String password) throws Exception {
		register(loginId, password, "境界 確認")
				.andExpect(status().isCreated());
	}

	static Stream<Arguments> rejectedPasswords() {
		return Stream.of(
				Arguments.of("pwng14", "abcdefghijklmn"), // 下限のすぐ下（14 文字）
				Arguments.of("pwng65", "y".repeat(65)), // 上限のすぐ上（65 文字）
				Arguments.of("pwngjp", "パスワードパスワードパスワード"), // ASCII 以外（BCrypt の 72 バイトを超えうる）
				Arguments.of("pwngtab", "abcdefgh\tijklmno")); // タブは 0x20〜0x7E の外
	}

	@ParameterizedTest
	@MethodSource("rejectedPasswords")
	void register_passwordOutsideRules_returns400(String loginId, String password) throws Exception {
		register(loginId, password, "境界 確認")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").value(PASSWORD_FORMAT));
	}

	// 15 文字で文字種も満たすが、空白だけのパスワードは未入力と同じに扱う。
	@Test
	void register_passwordOfOnlySpaces_returns400AsRequired() throws Exception {
		register("pwblank01", " ".repeat(15), "境界 確認")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.password").value("パスワードを入力してください"));
	}

	// 前後の空白を削らないこと: 登録した通りの値でだけログインでき、削った値ではログインできない。
	@Test
	void register_passwordWithSurroundingSpaces_isStoredAsTyped() throws Exception {
		String password = "  spaced pass phrase  ";
		register("trimcheck01", password, "空白 確認").andExpect(status().isCreated());

		mockMvc.perform(post("/api/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApiTestSupport.loginBody(jsonMapper, "trimcheck01", password)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApiTestSupport.loginBody(jsonMapper, "trimcheck01", password.strip())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void register_nameOfMaxLength_isAccepted() throws Exception {
		register("name50ok", VALID_PASSWORD, "名".repeat(50))
				.andExpect(status().isCreated());
	}

	static Stream<Arguments> rejectedNames() {
		return Stream.of(
				Arguments.of("名".repeat(51), "氏名は50文字以内で入力してください"), // 上限のすぐ上
				Arguments.of("山田\n太郎", "氏名に使用できない文字が含まれています"), // 改行（制御文字）
				Arguments.of("\u202E山田太郎", "氏名に使用できない文字が含まれています")); // 文字の向きを入れ替える書式文字
	}

	@ParameterizedTest
	@MethodSource("rejectedNames")
	void register_nameOutsideRules_returns400(String name, String expectedMessage) throws Exception {
		register("namecheck01", VALID_PASSWORD, name)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").value(expectedMessage));
	}

	// 前後の空白を削ると空になる氏名は未入力として扱う（全角スペースも削る）。
	@ParameterizedTest
	@ValueSource(strings = { "   ", "\u3000\u3000" })
	void register_nameOfOnlySpaces_returns400AsRequired(String name) throws Exception {
		register("nameblank01", VALID_PASSWORD, name)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").value("氏名を入力してください"));
	}

	@Test
	void register_nameWithSurroundingSpaces_isStoredStripped() throws Exception {
		register("namestrip01", VALID_PASSWORD, "\u3000 山田 花子 \u3000")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("山田 花子"));

		assertThat(userRepository.findByLoginId("namestrip01").orElseThrow().getName()).isEqualTo("山田 花子");
	}

	@Test
	void register_missingFields_returns400ForEachField() throws Exception {
		postRegistration("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.login_id").value("ログインIDを入力してください"))
				.andExpect(jsonPath("$.errors.password").value("パスワードを入力してください"))
				.andExpect(jsonPath("$.errors.name").value("氏名を入力してください"));
	}

	@Test
	void register_malformedJson_returns400() throws Exception {
		postRegistration("{\"login_id\": ")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("リクエストの形式が正しくありません"))
				.andExpect(jsonPath("$.errors").doesNotExist());
	}

	private ResultActions register(String loginId, String password, String name) throws Exception {
		return postRegistration(jsonMapper.writeValueAsString(registrationBody(loginId, password, name)));
	}

	private ResultActions postRegistration(String json) throws Exception {
		return mockMvc.perform(post("/api/users").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private static Map<String, String> registrationBody(String loginId, String password, String name) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("login_id", loginId);
		body.put("password", password);
		body.put("name", name);
		return body;
	}
}
