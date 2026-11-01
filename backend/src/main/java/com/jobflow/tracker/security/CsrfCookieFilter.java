package com.jobflow.tracker.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

// CSRF トークンは使われるまで作られない（Cookie も出ない）。最初の POST の前に React が Cookie を受け取れるよう、
// 毎回ここで読み込んで Cookie を出させる。認可より前に置くので、未ログインで 401 になる応答にも付く。
public class CsrfCookieFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (request.getAttribute(CsrfToken.class.getName()) instanceof CsrfToken csrfToken) {
			csrfToken.getToken();
		}
		filterChain.doFilter(request, response);
	}
}
