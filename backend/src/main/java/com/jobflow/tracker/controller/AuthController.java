package com.jobflow.tracker.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobflow.tracker.dto.LoginRequest;
import com.jobflow.tracker.dto.MessageResponse;
import com.jobflow.tracker.dto.UserResponse;
import com.jobflow.tracker.security.CredentialRules;
import com.jobflow.tracker.security.LoginSessionManager;
import com.jobflow.tracker.security.LoginUser;

@RestController
@RequestMapping("/api")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	private final AuthenticationManager authenticationManager;

	private final LoginSessionManager loginSessionManager;

	public AuthController(AuthenticationManager authenticationManager, LoginSessionManager loginSessionManager) {
		this.authenticationManager = authenticationManager;
		this.loginSessionManager = loginSessionManager;
	}

	// 失敗は BadCredentialsException にまとめ、ApiExceptionHandler が ID 違いもパスワード違いも同じ 401 にする。
	@PostMapping("/login")
	public UserResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		// 登録で受け付けない形なら DB もパスワードの照合（BCrypt）も通さずに断る。極端に長い入力で照合を走らせないため。
		if (!CredentialRules.isWellFormed(body.loginId(), body.password())) {
			throw new BadCredentialsException("Malformed credentials");
		}
		Authentication authentication = authenticationManager
				.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(body.loginId(), body.password()));
		loginSessionManager.start(authentication, request, response);

		LoginUser user = (LoginUser) authentication.getPrincipal();
		log.info("Login succeeded (user_id={})", user.getUserId());
		return UserResponse.from(user);
	}

	@PostMapping("/logout")
	public MessageResponse logout(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) {
		loginSessionManager.end(request, response, authentication);
		return MessageResponse.of("ログアウトしました");
	}
}
