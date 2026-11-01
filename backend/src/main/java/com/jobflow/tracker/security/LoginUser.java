package com.jobflow.tracker.security;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.Role;

// セッションに入るログイン中の利用者。Entity をセッションに入れると遅延読み込みや古い値の問題が出るので、必要な値だけ写す。
public final class LoginUser implements UserDetails, CredentialsContainer {

	@Serial
	private static final long serialVersionUID = 1L;

	private final Long userId;

	private final String loginId;

	private final String name;

	private final Role role;

	// 認証が済んだら ProviderManager が eraseCredentials() を呼んで消す。セッションにハッシュを残さないため。
	private String passwordHash;

	private LoginUser(Long userId, String loginId, String passwordHash, String name, Role role) {
		this.userId = userId;
		this.loginId = loginId;
		this.passwordHash = passwordHash;
		this.name = name;
		this.role = role;
	}

	public static LoginUser from(AppUser user) {
		return new LoginUser(user.getId(), user.getLoginId(), user.getPasswordHash(), user.getName(), user.getRole());
	}

	public Long getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}

	public Role getRole() {
		return role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return loginId;
	}

	@Override
	public void eraseCredentials() {
		passwordHash = null;
	}

	// Authentication の toString から呼ばれるので、ログイン ID・氏名・ハッシュは含めない。
	@Override
	public String toString() {
		return "LoginUser{userId=" + userId + ", role=" + role + "}";
	}
}
