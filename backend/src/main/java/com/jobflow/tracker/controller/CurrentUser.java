package com.jobflow.tracker.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.jobflow.tracker.security.LoginUser;

// 未ログインは Security の設定で先に 401 になる。ここで principal が無いのは想定外なので、データを返さず 401 にする。
final class CurrentUser {

	private CurrentUser() {
	}

	static Long requireId(LoginUser user) {
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		return user.getUserId();
	}
}
