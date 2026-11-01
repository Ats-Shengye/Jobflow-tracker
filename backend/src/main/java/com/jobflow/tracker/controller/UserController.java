package com.jobflow.tracker.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.jobflow.tracker.dto.RegisteredUserResponse;
import com.jobflow.tracker.dto.UserRegistrationRequest;
import com.jobflow.tracker.dto.UserResponse;
import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.security.LoginUser;
import com.jobflow.tracker.service.UserRegistrationService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserRegistrationService registrationService;

	public UserController(UserRegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	// 登録してもログインはさせない（詳細設計: 「登録しました。ログインしてください」でログイン画面へ）。
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RegisteredUserResponse register(@Valid @RequestBody UserRegistrationRequest body) {
		AppUser user = registrationService.registerSeeker(body.loginId(), body.password(), body.name());
		return RegisteredUserResponse.from(user);
	}

	// 未ログインは Security の設定で 401 になる。ここで principal が無いのは想定外なので、データを返さず 401 にする。
	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal LoginUser user) {
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		return UserResponse.from(user);
	}
}
