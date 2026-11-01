package com.jobflow.tracker.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import com.jobflow.tracker.dto.MessageResponse;

import tools.jackson.databind.json.JsonMapper;

// Spring Security の既定はログイン画面へのリダイレクトや HTML を返すので、React が扱える JSON にそろえる。
@Component
public class SecurityErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

	static final String UNAUTHENTICATED_MESSAGE = "ログインしてください";

	static final String FORBIDDEN_MESSAGE = "この操作を行う権限がありません";

	static final String CSRF_REJECTED_MESSAGE = "画面を読み込み直してから、もう一度操作してください";

	private static final Logger log = LoggerFactory.getLogger(SecurityErrorResponder.class);

	private final JsonMapper jsonMapper;

	public SecurityErrorResponder(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		write(response, HttpStatus.UNAUTHORIZED, UNAUTHENTICATED_MESSAGE);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		if (accessDeniedException instanceof CsrfException) {
			log.info("Rejected a request with a missing or invalid CSRF token");
			write(response, HttpStatus.FORBIDDEN, CSRF_REJECTED_MESSAGE);
			return;
		}
		log.info("Access denied by role");
		write(response, HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE);
	}

	private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		if (response.isCommitted()) {
			return;
		}
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getOutputStream(), MessageResponse.of(message));
	}
}
