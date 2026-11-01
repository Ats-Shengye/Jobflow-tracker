package com.jobflow.tracker.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

// status と面接回数の関係を DB なしで確かめる。保存まわりは Repository のテストで見る。
class JobApplicationTests {

	private static final AppUser OWNER = new AppUser("owner01", "test-hash", "テスト 利用者", Role.SEEKER);

	private static JobApplication newApplication(ApplicationStatus status, Integer interviewRound) {
		return new JobApplication(OWNER, "サンプル株式会社", "事務", EmploymentInsurance.YES, LocalDate.of(2026, 10, 1),
				status, interviewRound, null, null, null);
	}

	// 範囲の両端（1 と 5）は受け付ける。
	@ParameterizedTest
	@ValueSource(ints = { 1, 5 })
	void interviewRound_atInclusiveBoundary_isAccepted(int round) {
		JobApplication application = newApplication(ApplicationStatus.INTERVIEW, round);

		assertThat(application.getInterviewRound()).isEqualTo(round);
	}

	// 範囲のすぐ外（0 と 6）は、DTO のチェックをすり抜けても Entity で止める。
	@ParameterizedTest
	@ValueSource(ints = { 0, 6 })
	void interviewRound_outsideRange_isRejected(int round) {
		assertThatThrownBy(() -> newApplication(ApplicationStatus.INTERVIEW, round))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void interviewStatus_withoutRound_isRejected() {
		assertThatThrownBy(() -> newApplication(ApplicationStatus.INTERVIEW, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// 面接以外のステータスでは、回数を渡されても保存しない。
	@ParameterizedTest
	@EnumSource(value = ApplicationStatus.class, names = "INTERVIEW", mode = EnumSource.Mode.EXCLUDE)
	void nonInterviewStatus_withRound_storesNullRound(ApplicationStatus status) {
		JobApplication application = newApplication(status, 3);

		assertThat(application.getInterviewRound()).isNull();
	}

	// 面接から別のステータスに変えたら、回数は空になる。
	@Test
	void update_fromInterviewToOffered_clearsRound() {
		JobApplication application = newApplication(ApplicationStatus.INTERVIEW, 2);

		application.update("サンプル株式会社", "事務", EmploymentInsurance.YES, LocalDate.of(2026, 10, 1),
				ApplicationStatus.OFFERED, 2, null, null, null);

		assertThat(application.getStatus()).isEqualTo(ApplicationStatus.OFFERED);
		assertThat(application.getInterviewRound()).isNull();
	}

	// メモは私的な内容が入りうるので、toString 経由でログに出てはいけない。
	@Test
	void toString_doesNotContainMemo() {
		JobApplication application = new JobApplication(OWNER, "サンプル株式会社", "事務", EmploymentInsurance.YES,
				LocalDate.of(2026, 10, 1), ApplicationStatus.APPLIED, null, null, null, "secret-memo-text");

		assertThat(application.toString()).doesNotContain("secret-memo-text");
	}

	@Test
	void appUserToString_doesNotContainPasswordHashOrLoginId() {
		AppUser user = new AppUser("visible01", "hash-value-xyz", "テスト 利用者", Role.SEEKER);

		assertThat(user.toString()).doesNotContain("hash-value-xyz").doesNotContain("visible01");
	}
}
