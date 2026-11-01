package com.jobflow.tracker.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

import com.jobflow.tracker.entity.Role;
import com.jobflow.tracker.security.CsrfCookieFilter;
import com.jobflow.tracker.security.SecurityErrorResponder;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
			SecurityContextRepository securityContextRepository, CookieCsrfTokenRepository csrfTokenRepository,
			SecurityErrorResponder errorResponder) {
		http
				// 上から順に判定する。許可していない URL は最後の denyAll で拒否する。
				.authorizeHttpRequests(auth -> auth
						// 例外の転送先（/error）で認証を求めると、本来のエラーが 401 に化けるため。
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(HttpMethod.POST, "/api/users", "/api/login").permitAll()
						.requestMatchers("/api/admin/**").hasRole(Role.ADVISOR.name())
						// 担当者が求職者用の API を呼んだ時も 403 にする（担当者は閲覧のみで、応募を持たない）。
						.requestMatchers("/api/applications", "/api/applications/**").hasRole(Role.SEEKER.name())
						.requestMatchers("/api/**").authenticated()
						.anyRequest().denyAll())
				// spa() は Cookie のトークンを React がヘッダーで返す形の設定。Cookie に SameSite を付けるため保存先だけ差し替える。
				.csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(errorResponder)
						.accessDeniedHandler(errorResponder))
				// 既定のフォームログイン・Basic 認証・ログアウトは HTML やリダイレクトを返すので使わない。
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.requestCache(AbstractHttpConfigurer::disable)
				// JSON しか返さないので、応答の中で何も読み込ませず、他のサイトの枠にも入れさせない。
				.headers(headers -> headers
						.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")));
		return http.build();
	}

	// ログインの API とフィルターで同じ保存先を使う。リクエスト内とセッションの両方に保存する（Spring Security の既定と同じ組み合わせ）。
	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
				new HttpSessionSecurityContextRepository());
	}

	// React が読めるよう HttpOnly は付けない（トークンは秘密ではなく、他のサイトから読めないことで守る）。
	@Bean
	public CookieCsrfTokenRepository csrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> cookie.sameSite("Strict"));
		return repository;
	}

	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}
}
