package com.jobflow.tracker.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.Role;

class LoginUserTests {

	private static LoginUser newLoginUser() {
		return LoginUser.from(new AppUser("visible01", "hash-value-xyz", "表示 名前", Role.SEEKER));
	}

	// ロールは Spring Security の hasRole が見る「ROLE_」付きの名前で持つ。
	@Test
	void authorities_haveRolePrefix() {
		assertThat(newLoginUser().getAuthorities()).extracting(Object::toString).containsExactly("ROLE_SEEKER");
	}

	// 認証の後に呼ばれ、セッションにハッシュを残さない。
	@Test
	void eraseCredentials_removesPasswordHash() {
		LoginUser user = newLoginUser();

		user.eraseCredentials();

		assertThat(user.getPassword()).isNull();
	}

	// Authentication の toString 経由でログに出ても困らないこと。
	@Test
	void toString_doesNotContainLoginIdNameOrHash() {
		assertThat(newLoginUser().toString())
				.doesNotContain("visible01").doesNotContain("表示 名前").doesNotContain("hash-value-xyz");
	}
}
