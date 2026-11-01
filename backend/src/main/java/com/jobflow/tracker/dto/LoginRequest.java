package com.jobflow.tracker.dto;

import jakarta.validation.constraints.NotBlank;

// 文字数・文字種はここでは確かめない。登録で受け付けない形の ID・パスワードはどのアカウントとも一致しないので、
// 400 ではなく認証失敗（401）として返す（AuthController）。
public record LoginRequest(
		@NotBlank(message = ValidationMessages.LOGIN_REQUIRED) String loginId,
		@NotBlank(message = ValidationMessages.LOGIN_REQUIRED) String password) {

	// record の既定の toString は全項目を出すので、ログに混ざらないよう伏せる。
	@Override
	public String toString() {
		return "LoginRequest[loginId=***, password=***]";
	}
}
