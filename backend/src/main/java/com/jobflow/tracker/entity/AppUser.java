package com.jobflow.tracker.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Spring Security の User と紛れないよう AppUser にした。パスワードのハッシュが漏れないよう、JSON にはそのまま返さず DTO を使う。
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id"))
public class AppUser {

	// 登録の入力チェックもこの値を参照する。列長を変えたら既存の DB ファイルは作り直しが要る（ddl-auto=update は列長を変えない）。
	public static final int LOGIN_ID_MIN_LENGTH = 4;

	public static final int LOGIN_ID_MAX_LENGTH = 64;

	public static final int NAME_MAX_LENGTH = 50;

	// BCrypt のハッシュは60文字。別の方式に移っても列を変えずに済むよう余裕を持たせている。
	public static final int PASSWORD_HASH_MAX_LENGTH = 100;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(name = "login_id", nullable = false, length = LOGIN_ID_MAX_LENGTH)
	private String loginId;

	@Column(name = "password", nullable = false, length = PASSWORD_HASH_MAX_LENGTH)
	private String passwordHash;

	@Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private Role role;

	protected AppUser() {
	}

	// passwordHash には PasswordEncoder でハッシュ化した値を渡す（生のパスワードは渡さない）。
	public AppUser(String loginId, String passwordHash, String name, Role role) {
		this.loginId = Objects.requireNonNull(loginId, "loginId must not be null");
		this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
		this.name = Objects.requireNonNull(name, "name must not be null");
		this.role = Objects.requireNonNull(role, "role must not be null");
	}

	public Long getId() {
		return id;
	}

	public String getLoginId() {
		return loginId;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getName() {
		return name;
	}

	public Role getRole() {
		return role;
	}

	// ログに出ても困らないよう、パスワードのハッシュ・ログイン ID・氏名は含めない。
	@Override
	public String toString() {
		return "AppUser{id=" + id + ", role=" + role + "}";
	}
}
