package com.jobflow.tracker.dto;

import com.jobflow.tracker.entity.AppUser;

public record RegisteredUserResponse(Long userId, String loginId, String name) {

	public static RegisteredUserResponse from(AppUser user) {
		return new RegisteredUserResponse(user.getId(), user.getLoginId(), user.getName());
	}
}
