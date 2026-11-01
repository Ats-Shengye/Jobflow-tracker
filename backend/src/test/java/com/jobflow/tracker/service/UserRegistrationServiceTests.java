package com.jobflow.tracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.Role;
import com.jobflow.tracker.repository.AppUserRepository;

// 同時登録で existsByLoginId をすり抜けた時の判定。同時に2つ登録する状況は作りにくいので、
// 実際の H2 で UNIQUE 制約に違反させ、その例外をログイン ID の重複と判定できるかを見る。
@DataJpaTest
class UserRegistrationServiceTests {

	@Autowired
	private AppUserRepository userRepository;

	@Test
	void isUniqueViolation_duplicateLoginIdInsert_returnsTrue() {
		userRepository.saveAndFlush(new AppUser("race01", "test-hash", "同時 一号", Role.SEEKER));

		assertThatThrownBy(
				() -> userRepository.saveAndFlush(new AppUser("race01", "test-hash", "同時 二号", Role.SEEKER)))
				.isInstanceOfSatisfying(DataIntegrityViolationException.class,
						e -> assertThat(UserRegistrationService.isUniqueViolation(e)).isTrue());
	}

	// UNIQUE 以外の違反まで 409 にすると原因が隠れるので、重複とは判定しない。
	@Test
	void isUniqueViolation_otherDataIntegrityViolation_returnsFalse() {
		assertThat(UserRegistrationService.isUniqueViolation(new DataIntegrityViolationException("other")))
				.isFalse();
	}
}
