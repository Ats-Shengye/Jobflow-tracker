package com.jobflow.tracker.dto;

import com.jobflow.tracker.entity.Role;
import com.jobflow.tracker.security.LoginUser;

// ログインと GET /api/users/me の応答。画面は role で遷移先を決める。
public record UserResponse(Long userId, String name, Role role) {

	public static UserResponse from(LoginUser user) {
		return new UserResponse(user.getUserId(), user.getName(), user.getRole());
	}
}
