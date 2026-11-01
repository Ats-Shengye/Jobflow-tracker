package com.jobflow.tracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.ApplicationStatus;
import com.jobflow.tracker.entity.EmploymentInsurance;
import com.jobflow.tracker.entity.JobApplication;
import com.jobflow.tracker.entity.Role;

// 一覧の確認はテストで作った ID だけに絞って見るので、DB に他の行があっても結果は変わらない。
@DataJpaTest
class AppUserRepositoryTests {

	// Repository ではハッシュ化しないので、null でなければ何でもよい。
	private static final String PLACEHOLDER_HASH = "test-hash";

	@Autowired
	private AppUserRepository userRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void findByLoginId_existingLoginId_returnsUser() {
		AppUser saved = entityManager.persistAndFlush(new AppUser("finder01", PLACEHOLDER_HASH, "検索 太郎", Role.SEEKER));

		assertThat(userRepository.findByLoginId("finder01")).hasValueSatisfying(found -> {
			assertThat(found.getId()).isEqualTo(saved.getId());
			assertThat(found.getRole()).isEqualTo(Role.SEEKER);
		});
	}

	@Test
	void findByLoginId_unknownLoginId_returnsEmpty() {
		assertThat(userRepository.findByLoginId("nobody99")).isEmpty();
	}

	@Test
	void existsByLoginId_reflectsWhetherLoginIdIsTaken() {
		entityManager.persistAndFlush(new AppUser("taken01", PLACEHOLDER_HASH, "登録 済子", Role.SEEKER));

		assertThat(userRepository.existsByLoginId("taken01")).isTrue();
		assertThat(userRepository.existsByLoginId("free01")).isFalse();
	}

	// 同時登録で existsByLoginId をすり抜けた時の最後の砦が UNIQUE 制約。
	@Test
	void saveAndFlush_duplicateLoginId_isRejectedByUniqueConstraint() {
		userRepository.saveAndFlush(new AppUser("dup01", PLACEHOLDER_HASH, "重複 一号", Role.SEEKER));

		assertThatThrownBy(
				() -> userRepository.saveAndFlush(new AppUser("dup01", PLACEHOLDER_HASH, "重複 二号", Role.SEEKER)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	// 応募2件の求職者・0件の求職者・担当者を用意する。0件の人も件数0・最終更新 null で出て、担当者は出ないこと。
	@Test
	void findSeekerSummaries_aggregatesPerSeekerAndExcludesAdvisors() {
		AppUser busySeeker = entityManager.persist(new AppUser("busy01", PLACEHOLDER_HASH, "応募 多子", Role.SEEKER));
		AppUser idleSeeker = entityManager.persist(new AppUser("idle01", PLACEHOLDER_HASH, "応募 無子", Role.SEEKER));
		AppUser advisor = entityManager.persist(new AppUser("adv01", PLACEHOLDER_HASH, "担当 次郎", Role.ADVISOR));
		JobApplication first = entityManager.persistAndFlush(newApplication(busySeeker, LocalDate.of(2026, 10, 1)));
		JobApplication second = entityManager.persistAndFlush(newApplication(busySeeker, LocalDate.of(2026, 10, 2)));
		Instant expectedLastUpdate = first.getUpdatedAt().isAfter(second.getUpdatedAt()) ? first.getUpdatedAt()
				: second.getUpdatedAt();

		List<SeekerSummary> summaries = userRepository.findSeekerSummaries();

		Map<Long, SeekerSummary> byUserId = summaries.stream()
				.collect(Collectors.toMap(SeekerSummary::userId, Function.identity()));
		assertThat(byUserId).doesNotContainKey(advisor.getId());
		assertThat(byUserId.get(busySeeker.getId())).isEqualTo(
				new SeekerSummary(busySeeker.getId(), "応募 多子", 2L, expectedLastUpdate));
		assertThat(byUserId.get(idleSeeker.getId())).isEqualTo(
				new SeekerSummary(idleSeeker.getId(), "応募 無子", 0L, null));
		assertThat(summaries).extracting(SeekerSummary::userId).isSorted();
	}

	private static JobApplication newApplication(AppUser owner, LocalDate appliedDate) {
		return new JobApplication(owner, "サンプル株式会社", "事務", EmploymentInsurance.YES, appliedDate,
				ApplicationStatus.APPLIED, null, null, null, null);
	}
}
