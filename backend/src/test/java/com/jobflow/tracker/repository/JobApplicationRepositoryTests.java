package com.jobflow.tracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.ApplicationStatus;
import com.jobflow.tracker.entity.EmploymentInsurance;
import com.jobflow.tracker.entity.JobApplication;
import com.jobflow.tracker.entity.Role;

// @DataJpaTest は各テストの後にロールバックする。テーブルに実際に入った値を見たい所は JdbcTemplate で読む。
@DataJpaTest
class JobApplicationRepositoryTests {

	@Autowired
	private JobApplicationRepository applicationRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private AppUser owner;

	private AppUser otherUser;

	@BeforeEach
	void setUp() {
		// Repository ではハッシュ化しないので、null でなければ何でもよい。
		owner = entityManager.persist(new AppUser("ownerA01", "test-hash", "所有者 A", Role.SEEKER));
		otherUser = entityManager.persist(new AppUser("ownerB01", "test-hash", "所有者 B", Role.SEEKER));
	}

	private JobApplication persistApplication(AppUser user, LocalDate appliedDate) {
		return entityManager.persistAndFlush(new JobApplication(user, "サンプル株式会社", "事務", EmploymentInsurance.YES,
				appliedDate, ApplicationStatus.APPLIED, null, null, null, null));
	}

	@Test
	void findByIdAndOwnerId_ownApplication_returnsIt() {
		JobApplication application = persistApplication(owner, LocalDate.of(2026, 10, 1));

		assertThat(applicationRepository.findByIdAndOwnerId(application.getId(), owner.getId()))
				.hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(application.getId()));
	}

	// 他人の応募は、存在していても持ち主以外からは取れない（API では 404 になる）。
	@Test
	void findByIdAndOwnerId_otherUsersApplication_returnsEmpty() {
		JobApplication application = persistApplication(owner, LocalDate.of(2026, 10, 1));

		assertThat(applicationRepository.findByIdAndOwnerId(application.getId(), otherUser.getId())).isEmpty();
	}

	@Test
	void findByIdAndOwnerId_unknownId_returnsEmpty() {
		assertThat(applicationRepository.findByIdAndOwnerId(Long.MAX_VALUE, owner.getId())).isEmpty();
	}

	// 申込日の降順、同じ日なら ID の降順。2026-10-10 を2件にして同じ日の並びも見る。他人の応募は出ないこと。
	@Test
	void findByOwnerIdOrderBy_returnsOnlyOwnApplicationsNewestFirst() {
		JobApplication oldest = persistApplication(owner, LocalDate.of(2026, 10, 1));
		JobApplication newest = persistApplication(owner, LocalDate.of(2026, 10, 20));
		JobApplication middleFirst = persistApplication(owner, LocalDate.of(2026, 10, 10));
		JobApplication middleSecond = persistApplication(owner, LocalDate.of(2026, 10, 10));
		persistApplication(otherUser, LocalDate.of(2026, 10, 25));

		assertThat(applicationRepository.findByOwnerIdOrderByAppliedDateDescIdDesc(owner.getId()))
				.extracting(JobApplication::getId)
				.containsExactly(newest.getId(), middleSecond.getId(), middleFirst.getId(), oldest.getId());
	}

	// enum は番号ではなく英字コードの文字列で保存される。
	@Test
	void save_storesEnumsAsEnglishCodeStrings() {
		JobApplication application = entityManager.persistAndFlush(new JobApplication(owner, "サンプル株式会社", "事務",
				EmploymentInsurance.UNKNOWN, LocalDate.of(2026, 10, 1), ApplicationStatus.INTERVIEW, 2, null, null,
				null));

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select status, employment_insurance, interview_round from applications where application_id = ?",
				application.getId());
		String role = jdbcTemplate.queryForObject("select role from users where user_id = ?", String.class,
				owner.getId());

		assertThat(row.get("status")).isEqualTo("INTERVIEW");
		assertThat(row.get("employment_insurance")).isEqualTo("UNKNOWN");
		assertThat(((Number) row.get("interview_round")).intValue()).isEqualTo(2);
		assertThat(role).isEqualTo("SEEKER");
	}

	// 全角英字と機種依存文字（㈱）を含む会社名が、保存して読み直しても変わらないこと。
	@Test
	void save_companyNameWithFullWidthAndCircledCharacters_readsBackUnchanged() {
		String companyName = "㈱Ａ商事";
		JobApplication application = entityManager.persistAndFlush(new JobApplication(owner, companyName, "事務",
				EmploymentInsurance.YES, LocalDate.of(2026, 10, 1), ApplicationStatus.APPLIED, null, null, null, null));
		entityManager.clear();

		JobApplication reloaded = applicationRepository.findByIdAndOwnerId(application.getId(), owner.getId())
				.orElseThrow();

		assertThat(reloaded.getCompanyName()).isEqualTo(companyName);
	}

	@Test
	void save_newApplication_recordsUpdatedAt() {
		Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);

		JobApplication application = persistApplication(owner, LocalDate.of(2026, 10, 1));

		Instant after = Instant.now();
		Integer storedRows = jdbcTemplate.queryForObject(
				"select count(*) from applications where application_id = ? and updated_at is not null", Integer.class,
				application.getId());
		assertThat(application.getUpdatedAt()).isBetween(before, after);
		assertThat(storedRows).isEqualTo(1);
	}

	// 時計を2回読んで差が出ることに頼らないよう、先に SQL で updated_at を1日前にずらしておく。
	@Test
	void update_changedApplication_refreshesUpdatedAt() {
		JobApplication application = persistApplication(owner, LocalDate.of(2026, 10, 1));
		Long id = application.getId();
		jdbcTemplate.update("update applications set updated_at = updated_at - INTERVAL '1' DAY where application_id = ?",
				id);
		entityManager.clear();
		JobApplication loaded = applicationRepository.findByIdAndOwnerId(id, owner.getId()).orElseThrow();
		Instant previous = loaded.getUpdatedAt();

		loaded.update("サンプル株式会社", "事務", EmploymentInsurance.YES, LocalDate.of(2026, 10, 1),
				ApplicationStatus.DOCUMENT_SCREENING, null, null, null, "更新後のメモ");
		entityManager.flush();
		entityManager.clear();

		JobApplication reloaded = applicationRepository.findByIdAndOwnerId(id, owner.getId()).orElseThrow();
		assertThat(reloaded.getUpdatedAt()).isAfter(previous);
		assertThat(reloaded.getStatus()).isEqualTo(ApplicationStatus.DOCUMENT_SCREENING);
		assertThat(reloaded.getMemo()).isEqualTo("更新後のメモ");
	}

	// 初期データ用の一括 update は @PreUpdate を通らないので、渡した過去の日時がそのまま入り、他の行は変わらないこと。
	@Test
	void overwriteUpdatedAtForSeedData_setsGivenInstantOnlyOnThatRow() {
		JobApplication target = persistApplication(owner, LocalDate.of(2026, 7, 12));
		JobApplication untouched = persistApplication(owner, LocalDate.of(2026, 7, 12));
		Instant untouchedBefore = untouched.getUpdatedAt();
		Instant pastInstant = Instant.parse("2026-07-17T01:20:00Z");

		// 持ち主が違えば書き換えない。
		int rowsForOtherUser = applicationRepository.overwriteUpdatedAtForSeedData(target.getId(), otherUser.getId(),
				pastInstant);
		int updatedRows = applicationRepository.overwriteUpdatedAtForSeedData(target.getId(), owner.getId(),
				pastInstant);
		entityManager.clear();

		assertThat(rowsForOtherUser).isZero();
		assertThat(updatedRows).isEqualTo(1);
		assertThat(applicationRepository.findByIdAndOwnerId(target.getId(), owner.getId()).orElseThrow()
				.getUpdatedAt()).isEqualTo(pastInstant);
		assertThat(applicationRepository.findByIdAndOwnerId(untouched.getId(), owner.getId()).orElseThrow()
				.getUpdatedAt()).isEqualTo(untouchedBefore);
	}

	@Test
	void delete_ownApplication_removesOnlyThatRow() {
		JobApplication toDelete = persistApplication(owner, LocalDate.of(2026, 10, 1));
		persistApplication(owner, LocalDate.of(2026, 10, 2));
		persistApplication(otherUser, LocalDate.of(2026, 10, 3));

		applicationRepository.delete(toDelete);
		entityManager.flush();

		assertThat(applicationRepository.countByOwnerId(owner.getId())).isEqualTo(1);
		assertThat(applicationRepository.countByOwnerId(otherUser.getId())).isEqualTo(1);
		assertThat(applicationRepository.findByIdAndOwnerId(toDelete.getId(), owner.getId())).isEmpty();
	}
}
