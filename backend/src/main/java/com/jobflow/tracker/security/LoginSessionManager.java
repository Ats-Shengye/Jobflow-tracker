package com.jobflow.tracker.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

// /api/login を自前で書いたので、formLogin と logout が自動でやっていることをここで行う。
@Component
public class LoginSessionManager {

	// Spring Boot の既定のセッション Cookie 名（server.servlet.session.cookie.name を変えたらここも変える）。
	static final String SESSION_COOKIE_NAME = "JSESSIONID";

	private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

	private final SessionAuthenticationStrategy sessionFixationProtection = new ChangeSessionIdAuthenticationStrategy();

	private final SecurityContextLogoutHandler contextLogoutHandler = new SecurityContextLogoutHandler();

	private final CookieClearingLogoutHandler sessionCookieClearer = new CookieClearingLogoutHandler(
			SESSION_COOKIE_NAME);

	private final SecurityContextRepository securityContextRepository;

	private final CsrfTokenRepository csrfTokenRepository;

	public LoginSessionManager(SecurityContextRepository securityContextRepository,
			CsrfTokenRepository csrfTokenRepository) {
		this.securityContextRepository = securityContextRepository;
		this.csrfTokenRepository = csrfTokenRepository;
		this.contextLogoutHandler.setSecurityContextRepository(securityContextRepository);
	}

	public void start(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
		// ログイン前のセッション ID を使い続けると、攻撃者が仕込んだ ID でログインさせられる（セッション固定攻撃）。
		sessionFixationProtection.onAuthentication(authentication, request, response);

		// Spring Security 6 以降は自動で保存しないので、保存しないと次のリクエストで未ログインに戻る。
		SecurityContext context = contextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		contextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);

		rotateCsrfToken(request, response);
	}

	public void end(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
		contextLogoutHandler.logout(request, response, authentication);
		sessionCookieClearer.logout(request, response, authentication);
		rotateCsrfToken(request, response);
	}

	// formLogin・logout と同じく、ログイン・ログアウトの前後で CSRF トークンを入れ替える。
	// 新しいトークンを同じ応答の Cookie で返し、React が GET を挟まずに次の POST を送れるようにする。
	private void rotateCsrfToken(HttpServletRequest request, HttpServletResponse response) {
		csrfTokenRepository.saveToken(csrfTokenRepository.generateToken(request), request, response);
	}
}
