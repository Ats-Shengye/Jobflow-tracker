package com.jobflow.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.security.CredentialRules;

// role は項目に持たない。本文に role が入っていても読まれず、登録は必ず求職者になる。
// 知らないキーで 400 にしないのは、role を送られても無視して作る、という決めごとに合わせるため。
public record UserRegistrationRequest(
		@NotBlank(message = ValidationMessages.LOGIN_ID_REQUIRED)
		@Pattern(regexp = CredentialRules.LOGIN_ID_PATTERN, message = ValidationMessages.LOGIN_ID_FORMAT)
		String loginId,

		@NotBlank(message = ValidationMessages.PASSWORD_REQUIRED)
		@Pattern(regexp = CredentialRules.PASSWORD_PATTERN, message = ValidationMessages.PASSWORD_FORMAT)
		String password,

		// 制御文字と書式文字（文字の向きを入れ替える U+202E など）は、担当者の一覧で表示を崩せるので弾く。
		@NotBlank(message = ValidationMessages.NAME_REQUIRED)
		@Size(max = AppUser.NAME_MAX_LENGTH, message = ValidationMessages.NAME_TOO_LONG)
		@Pattern(regexp = "[^\\p{Cc}\\p{Cf}\\p{Cs}]*", message = ValidationMessages.NAME_INVALID_CHARACTER)
		String name) {

	// 氏名だけ前後の空白（全角スペースを含む）を削ってから確かめる。コピー＆ペーストで混ざりやすく、残す意味が無いため。
	// ログイン ID とパスワードは削らない（ID は空白を含められないので弾くだけ、パスワードは空白も値のうち）。
	public UserRegistrationRequest {
		if (name != null) {
			name = name.strip();
		}
	}

	@Override
	public String toString() {
		return "UserRegistrationRequest[loginId=***, password=***, name=***]";
	}
}
