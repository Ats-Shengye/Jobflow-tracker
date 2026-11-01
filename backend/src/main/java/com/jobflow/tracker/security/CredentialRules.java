package com.jobflow.tracker.security;

import java.util.regex.Pattern;

import com.jobflow.tracker.entity.AppUser;

// ログイン ID とパスワードの形。登録の入力チェックとログイン前の確認で同じものを使う。
public final class CredentialRules {

	public static final int PASSWORD_MIN_LENGTH = 15;

	// ASCII だけなので 64 文字でも BCrypt の上限（72 バイト）に収まる。
	public static final int PASSWORD_MAX_LENGTH = 64;

	// 0x21〜0x7E は空白を除く ASCII の印字可能文字。大文字小文字は区別し、正規化しない。
	public static final String LOGIN_ID_PATTERN = "[\\x21-\\x7E]{" + AppUser.LOGIN_ID_MIN_LENGTH + ","
			+ AppUser.LOGIN_ID_MAX_LENGTH + "}";

	// パスフレーズのため半角スペース（0x20）も許す。前後の空白も削らない（登録時とログイン時で値がずれないように）。
	public static final String PASSWORD_PATTERN = "[\\x20-\\x7E]{" + PASSWORD_MIN_LENGTH + "," + PASSWORD_MAX_LENGTH
			+ "}";

	private static final Pattern LOGIN_ID = Pattern.compile(LOGIN_ID_PATTERN);

	private static final Pattern PASSWORD = Pattern.compile(PASSWORD_PATTERN);

	private CredentialRules() {
	}

	// 登録で受け付けない形の ID・パスワードは、どのアカウントとも一致しない。
	public static boolean isWellFormed(String loginId, String password) {
		return loginId != null && password != null && LOGIN_ID.matcher(loginId).matches()
				&& PASSWORD.matcher(password).matches() && !password.isBlank();
	}
}
