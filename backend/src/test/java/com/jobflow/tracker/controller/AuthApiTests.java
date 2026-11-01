package com.jobflow.tracker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import com.jobflow.tracker.security.LoginUser;

import tools.jackson.databind.json.JsonMapper;

// ログイン・ログアウト・ログイン中の利用者の取得と、URL ごとの認可を、Security のフィルターを通して確かめる。
@SpringBootTest
class AuthApiTests {

	private static final String CSRF_COOKIE = "XSRF-TOKEN";

	private static final String LOGIN_FAILED = "ログインIDまたはパスワードが正しくありません";

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private JsonMapper jsonMapper;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = ApiTestSupport.mockMvc(context);
	}

	// 画面の起動時にまず呼ぶ API。未ログインでも、次のログインに使う CSRF トークンの Cookie が届く必要がある。
	@Test
	void me_withoutLogin_returns401JsonAndIssuesReadableCsrfCookie() throws Exception {
		mockMvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("ログインしてください"))
				.andExpect(cookie().exists(CSRF_COOKIE))
				// React が読んでヘッダーに入れるので HttpOnly ではない。他のサイトからのリクエストには付けさせない。
				.andExpect(cookie().httpOnly(CSRF_COOKIE, false))
				.andExpect(cookie().attribute(CSRF_COOKIE, "SameSite", "Strict"));
	}

	// ブラウザと同じ手順: Cookie で受け取ったトークンを X-XSRF-TOKEN ヘッダーで返してログインする。
	// ログインの応答では CSRF トークンが入れ替わり、新しい Cookie が届く。
	@Test
	void login_withTokenFromCsrfCookie_returnsUserAndRotatesToken() throws Exception {
		Cookie issued = mockMvc.perform(get("/api/users/me")).andReturn().getResponse().getCookie(CSRF_COOKIE);
		assertThat(issued).isNotNull();

		MvcResult result = mockMvc.perform(post("/api/login")
				.cookie(issued)
				.header("X-XSRF-TOKEN", issued.getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApiTestSupport.loginBody(jsonMapper, ApiTestSupport.SEEKER_LOGIN_ID,
						ApiTestSupport.SEEKER_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.user_id").isNumber())
				.andExpect(jsonPath("$.name").value("山田 太郎"))
				.andExpect(jsonPath("$.role").value("SEEKER"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andReturn();

		Cookie rotated = result.getResponse().getCookie(CSRF_COOKIE);
		assertThat(rotated).isNotNull();
		assertThat(rotated.getValue()).isNotEmpty().isNotEqualTo(issued.getValue());
	}

	@Test
	void login_asAdvisor_returnsAdvisorRole() throws Exception {
		mockMvc.perform(post("/api/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApiTestSupport.loginBody(jsonMapper, ApiTestSupport.ADVISOR_LOGIN_ID,
						ApiTestSupport.ADVISOR_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADVISOR"));
	}

	// 応答の違いから登録済みのログイン ID を推測されないよう、ID 違いとパスワード違いで本文まで同じであること。
	@Test
	void login_wrongPasswordAndUnknownLoginId_returnIdenticalResponses() throws Exception {
		String wrongPassword = mockMvc.perform(post("/api/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApiTestSupport.loginBody(jsonMapper, ApiTestSupport.SEEKER_LOGIN_ID,
						"wrong-password-2026")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(LOGIN_FAILED))
				.andReturn().getResponse().getContentAsString();

		String unknownId = mockMvc.perform(post("/api/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApiTestSupport.loginBody(jsonMapper, "nobody-here-99", "wrong-password-2026")))
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		assertThat(unknownId).isEqualTo(wrongPassword);
	}

	// ログイン画面の未入力の文言は1つなので、どちらの項目にも同じ文言を返す。
	@Test
	void login_blankFields_returns400WithLoginScreenMessage() throws Exception {
		mockMvc.perform(post("/api/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApiTestSupport.loginBody(jsonMapper, "", " ")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.login_id").value("ログインIDとパスワードを入力してください"))
				.andExpect(jsonPath("$.errors.password").value("ログインIDとパスワードを入力してください"));
	}

	// 65 文字はパスワードの上限を超えるのでどのアカウントとも一致しない。照合をせずに認証失敗として返す。
	@Test
	void login_passwordLongerThanLimit_returns401() throws Exception {
		mockMvc.perform(post("/api/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApiTestSupport.loginBody(jsonMapper, ApiTestSupport.SEEKER_LOGIN_ID, "a".repeat(65))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(LOGIN_FAILED));
	}

	// セッション固定攻撃の対策: ログイン前のセッション ID がログイン後に使えないこと。
	// あわせて、ログイン情報がセッションに保存され、次のリクエストでもログイン中として扱われること。
	@Test
	void login_changesSessionIdAndKeepsUserLoggedInOnNextRequest() throws Exception {
		MockHttpSession session = new MockHttpSession();
		String sessionIdBeforeLogin = session.getId();

		// ブラウザがログイン前のセッション ID を送ってきた状態にする。
		// 送られていないと、Spring Security は新しく作ったセッションとみなして ID を振り直さない。
		mockMvc.perform(post("/api/login").session(session).with(csrf())
				.with(request -> {
					request.setRequestedSessionId(sessionIdBeforeLogin);
					return request;
				})
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApiTestSupport.loginBody(jsonMapper, ApiTestSupport.SEEKER_LOGIN_ID,
						ApiTestSupport.SEEKER_PASSWORD)))
				.andExpect(status().isOk());

		assertThat(session.getId()).isNotEqualTo(sessionIdBeforeLogin);
		mockMvc.perform(get("/api/users/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("山田 太郎"))
				.andExpect(jsonPath("$.role").value("SEEKER"));
	}

	// セッションに入るログイン情報に、パスワードのハッシュが残っていないこと。
	@Test
	void login_storesPrincipalWithoutPasswordHashInSession() throws Exception {
		MockHttpSession session = ApiTestSupport.login(mockMvc, jsonMapper, ApiTestSupport.SEEKER_LOGIN_ID,
				ApiTestSupport.SEEKER_PASSWORD);

		SecurityContext securityContext = (SecurityContext) session
				.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		assertThat(securityContext).isNotNull();
		assertThat(securityContext.getAuthentication().getCredentials()).isNull();
		assertThat(securityContext.getAuthentication().getPrincipal())
				.isInstanceOfSatisfying(LoginUser.class, user -> assertThat(user.getPassword()).isNull());
	}

	@Test
	void logout_invalidatesSessionAndClearsSessionCookie() throws Exception {
		MockHttpSession session = ApiTestSupport.login(mockMvc, jsonMapper, ApiTestSupport.SEEKER_LOGIN_ID,
				ApiTestSupport.SEEKER_PASSWORD);

		mockMvc.perform(post("/api/logout").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("ログアウトしました"))
				.andExpect(cookie().maxAge("JSESSIONID", 0))
				.andExpect(cookie().exists(CSRF_COOKIE));

		assertThat(session.isInvalid()).isTrue();
		mockMvc.perform(get("/api/users/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void logout_withoutLogin_returns401() throws Exception {
		mockMvc.perform(post("/api/logout").with(csrf()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("ログインしてください"));
	}

	// 認証の要らない POST でも、CSRF トークンが無ければ受け付けない（ログイン CSRF・勝手な登録の対策）。
	@Test
	void post_withoutCsrfToken_returns403Json() throws Exception {
		mockMvc.perform(post("/api/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(ApiTestSupport.loginBody(jsonMapper, ApiTestSupport.SEEKER_LOGIN_ID,
						ApiTestSupport.SEEKER_PASSWORD)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("画面を読み込み直してから、もう一度操作してください"));

		mockMvc.perform(post("/api/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"login_id\":\"nocsrf01\",\"password\":\"correct horse battery\",\"name\":\"確認 用\"}"))
				.andExpect(status().isForbidden());
	}

	// 既定の Spring Security はログイン画面へリダイレクトするので、401 の JSON でリダイレクトしないこと。
	@Test
	void protectedApi_withoutLogin_returns401JsonWithoutRedirect() throws Exception {
		mockMvc.perform(get("/api/applications"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("ログインしてください"));
	}

	@Test
	void seeker_onAdvisorApi_returns403() throws Exception {
		mockMvc.perform(get("/api/admin/seekers").with(user("seeker-x").roles("SEEKER")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("この操作を行う権限がありません"));
	}

	// 担当者は閲覧のみで応募を持たないので、求職者用の API は 403。
	@Test
	void advisor_onSeekerApi_returns403() throws Exception {
		mockMvc.perform(get("/api/applications").with(user("advisor-x").roles("ADVISOR")))
				.andExpect(status().isForbidden());
	}

	// 担当者の役割があれば認可を通り、求職者一覧が返る（401・403 にならないことを見る）。
	@Test
	void advisor_onAdvisorApi_passesAuthorization() throws Exception {
		mockMvc.perform(get("/api/admin/seekers").with(user("advisor-x").roles("ADVISOR")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}
}
