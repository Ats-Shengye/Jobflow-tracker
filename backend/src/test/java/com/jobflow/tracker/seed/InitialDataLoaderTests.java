package com.jobflow.tracker.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.ApplicationStatus;
import com.jobflow.tracker.entity.JobApplication;
import com.jobflow.tracker.entity.Role;
import com.jobflow.tracker.repository.AppUserRepository;
import com.jobflow.tracker.repository.JobApplicationRepository;
import com.jobflow.tracker.repository.SeekerSummary;
import com.jobflow.tracker.seed.DemoData.DemoApplication;
import com.jobflow.tracker.seed.DemoData.DemoSeeker;

// 起動時に一度シードが走った状態を確かめ、もう一度 run して二重にならないことを見る。
// JobflowTrackerApplicationTests と同じ設定（素の @SpringBootTest）のままにすること。テスト用の H2 は JVM の中で
// 共有されるので、設定の違うコンテキストが増えると create-drop で先のコンテキストのテーブルが消される。
// 他のテストで作った求職者も DB にいるので、求職者の確認はデモのログイン ID の人に絞る。
@SpringBootTest
class InitialDataLoaderTests {

	private static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");

	private static final List<String> ADDED_SEEKER_LOGIN_IDS = List.of("seeker02", "seeker03", "seeker04",
			"seeker05", "seeker06", "seeker07");

	private static final List<String> DEMO_SEEKER_LOGIN_IDS = Stream
			.concat(Stream.of(InitialDataLoader.SEEKER_LOGIN_ID), ADDED_SEEKER_LOGIN_IDS.stream())
			.toList();

	// 申込日と最終更新日に使ってよい18日。DemoData とは別に持ち、定義の打ち間違いも拾う。
	private static final Set<LocalDate> ACTIVITY_DATES = Stream.of("2026-07-12", "2026-07-13", "2026-07-17",
			"2026-07-25", "2026-08-04", "2026-08-10", "2026-08-15", "2026-08-22", "2026-08-24", "2026-09-03",
			"2026-09-12", "2026-09-18", "2026-09-26", "2026-10-02", "2026-10-10", "2026-10-12", "2026-10-16",
			"2026-10-30")
			.map(LocalDate::parse)
			.collect(Collectors.toUnmodifiableSet());

	@Autowired
	private InitialDataLoader loader;

	@Autowired
	private AppUserRepository userRepository;

	@Autowired
	private JobApplicationRepository applicationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void startup_createsAdvisorWithHashedPassword() {
		AppUser advisor = userRepository.findByLoginId(InitialDataLoader.ADVISOR_LOGIN_ID).orElseThrow();

		assertThat(advisor.getRole()).isEqualTo(Role.ADVISOR);
		assertThat(advisor.getName()).isEqualTo(InitialDataLoader.ADVISOR_NAME);
		assertPasswordIsBcryptHashOf(advisor, InitialDataLoader.ADVISOR_PASSWORD);
		assertThat(applicationRepository.countByOwnerId(advisor.getId())).isZero();
	}

	@Test
	void startup_createsDemoSeekerWithHashedPassword() {
		AppUser seeker = userRepository.findByLoginId(InitialDataLoader.SEEKER_LOGIN_ID).orElseThrow();

		assertThat(seeker.getRole()).isEqualTo(Role.SEEKER);
		assertThat(seeker.getName()).isEqualTo(InitialDataLoader.SEEKER_NAME);
		assertPasswordIsBcryptHashOf(seeker, InitialDataLoader.SEEKER_PASSWORD);
	}

	// スクリーンショット用なので、全ステータスと、回数の入った面接がそろっていること。申込日は元の6日のまま。
	@Test
	void startup_createsDemoApplicationsCoveringEveryStatus() {
		AppUser seeker = userRepository.findByLoginId(InitialDataLoader.SEEKER_LOGIN_ID).orElseThrow();

		List<JobApplication> applications = applicationRepository
				.findByOwnerIdOrderByAppliedDateDescIdDesc(seeker.getId());

		assertThat(applications).extracting(JobApplication::getStatus)
				.containsExactlyInAnyOrder(ApplicationStatus.values());
		assertThat(applications).filteredOn(a -> a.getStatus() == ApplicationStatus.INTERVIEW)
				.allSatisfy(a -> assertThat(a.getInterviewRound()).isNotNull());
		assertThat(applications).allSatisfy(a -> assertThat(a.getUpdatedAt()).isNotNull());
		assertThat(applications).extracting(JobApplication::getAppliedDate).containsExactly(
				LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 10),
				LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 1));
	}

	// 管理No（user_id）が要る場面を見せるため、字まで同じ「山田 太郎」が2人と、同じ読みの人がそろっていること。
	@Test
	void startup_createsSevenDemoSeekersIncludingSameNames() {
		List<AppUser> seekers = demoSeekers();

		assertThat(seekers).hasSize(7).allSatisfy(s -> assertThat(s.getRole()).isEqualTo(Role.SEEKER));
		assertThat(seekers).extracting(AppUser::getName).containsExactly("山田 太郎", "山田 太朗", "山田 汰朗", "山田 太郎",
				"佐藤 一郎", "佐藤 一朗", "佐藤 壱朗");
		assertThat(seekers).extracting(AppUser::getId).doesNotHaveDuplicates();
	}

	// 応募0件の人は一覧の最終更新日が空（null）になる。その人が2人いて、他の人は1件以上あること。
	@Test
	void startup_includesTwoDemoSeekersWithoutApplications() {
		List<SeekerSummary> summaries = demoSeekerSummaries();

		assertThat(summaries).hasSize(7);
		assertThat(summaries).filteredOn(s -> s.applicationCount() == 0).hasSize(2)
				.allSatisfy(s -> assertThat(s.lastUpdatedAt()).isNull());
		assertThat(summaries).filteredOn(s -> s.applicationCount() > 0).hasSize(5)
				.allSatisfy(s -> assertThat(s.lastUpdatedAt()).isNotNull());
	}

	// 足した6人は README のパスワードでも空のパスワードでもログインできないこと（乱数のパスワードはどこにも残していない）。
	@Test
	void startup_addedSeekersAcceptNeitherReadmeNorEmptyPassword() {
		List<String> guesses = List.of(InitialDataLoader.SEEKER_PASSWORD, InitialDataLoader.ADVISOR_PASSWORD, "");

		for (String loginId : ADDED_SEEKER_LOGIN_IDS) {
			String hash = userRepository.findByLoginId(loginId).orElseThrow().getPasswordHash();

			assertThat(hash).startsWith("$2");
			assertThat(guesses).noneMatch(guess -> passwordEncoder.matches(guess, hash));
		}
	}

	// 最終更新日時は保存した時刻ではなく、決めた日時に書き換わっていること。
	// どれも日本時間で18日のどれかの日中（9〜20時）で、申込日より前にならない。足した6人は申込日も18日のどれか。
	@Test
	void startup_rewritesUpdatedAtToDaytimeActivityDatesNotBeforeAppliedDate() {
		assertCompanyAndUpdatedAt(InitialDataLoader.SEEKER_LOGIN_ID, DemoData.MAIN_SEEKER_APPLICATIONS);
		for (DemoSeeker demo : DemoData.EXTRA_SEEKERS) {
			assertCompanyAndUpdatedAt(demo.loginId(), demo.applications());
		}

		for (AppUser seeker : demoSeekers()) {
			assertThat(applicationsOf(seeker)).allSatisfy(application -> {
				LocalDateTime updatedInJapan = LocalDateTime.ofInstant(application.getUpdatedAt(), JAPAN);
				assertThat(updatedInJapan.toLocalDate()).isIn(ACTIVITY_DATES)
						.isAfterOrEqualTo(application.getAppliedDate());
				assertThat(updatedInJapan.toLocalTime()).isBetween(LocalTime.of(9, 0), LocalTime.of(20, 0));
			});
		}
		for (String loginId : ADDED_SEEKER_LOGIN_IDS) {
			assertThat(applicationsOf(userRepository.findByLoginId(loginId).orElseThrow()))
					.allSatisfy(application -> assertThat(application.getAppliedDate()).isIn(ACTIVITY_DATES));
		}
	}

	// 古い順にした時に並びがはっきり変わるよう、応募のある5人の最新の最終更新日は全員違う日で、
	// seeker01 が一番新しく活動していること。
	@Test
	void startup_latestUpdatePerSeekerDiffersAndMainSeekerIsNewest() {
		Long mainSeekerId = userRepository.findByLoginId(InitialDataLoader.SEEKER_LOGIN_ID).orElseThrow().getId();
		List<SeekerSummary> withApplications = demoSeekerSummaries().stream()
				.filter(s -> s.lastUpdatedAt() != null)
				.toList();
		Instant mainSeekerLatest = withApplications.stream()
				.filter(s -> s.userId().equals(mainSeekerId))
				.map(SeekerSummary::lastUpdatedAt)
				.findFirst()
				.orElseThrow();

		assertThat(withApplications).extracting(s -> LocalDate.ofInstant(s.lastUpdatedAt(), JAPAN))
				.hasSize(5)
				.doesNotHaveDuplicates();
		assertThat(withApplications).filteredOn(s -> !s.userId().equals(mainSeekerId))
				.allSatisfy(s -> assertThat(s.lastUpdatedAt()).isBefore(mainSeekerLatest));
	}

	// 再起動のつもりでもう一度 run しても、アカウントも応募も増えず、最終更新日時も書き換わらないこと。
	@Test
	void run_whenDataAlreadyExists_createsNothing() {
		long userCountBefore = userRepository.count();
		Map<String, List<Instant>> updatedAtBefore = updatedAtByDemoLoginId();

		loader.run(new DefaultApplicationArguments());

		assertThat(userRepository.count()).isEqualTo(userCountBefore);
		assertThat(updatedAtByDemoLoginId()).isEqualTo(updatedAtBefore);
	}

	// 担当者だけ消えていたら担当者だけ作り直し、既にいる求職者にデモ応募を足さないこと。
	// 削除は @Transactional でロールバックされるので、他のテストには影響しない。
	@Test
	@Transactional
	void run_whenOnlyAdvisorMissing_recreatesAdvisorWithoutDuplicatingDemoApplications() {
		userRepository.delete(userRepository.findByLoginId(InitialDataLoader.ADVISOR_LOGIN_ID).orElseThrow());
		userRepository.flush();
		AppUser seeker = userRepository.findByLoginId(InitialDataLoader.SEEKER_LOGIN_ID).orElseThrow();
		long demoApplicationsBefore = applicationRepository.countByOwnerId(seeker.getId());

		loader.run(new DefaultApplicationArguments());

		assertThat(userRepository.findByLoginId(InitialDataLoader.ADVISOR_LOGIN_ID))
				.hasValueSatisfying(advisor -> assertThat(advisor.getRole()).isEqualTo(Role.ADVISOR));
		assertThat(applicationRepository.countByOwnerId(seeker.getId())).isEqualTo(demoApplicationsBefore);
	}

	// 起動直後は担当者・求職者のどちらも README のパスワードでログインできる状態なので、両方が警告の対象。
	// 足した6人は README にパスワードが無いので対象外。
	@Test
	void activeDemoAccountRoles_afterStartup_includesBothDemoAccounts() {
		assertThat(loader.activeDemoAccountRoles()).containsExactly(Role.ADVISOR, Role.SEEKER);
	}

	// アカウントが無ければ警告の対象から外れること。削除は @Transactional でロールバックされる。
	@Test
	@Transactional
	void activeDemoAccountRoles_whenAdvisorRemoved_listsOnlySeeker() {
		userRepository.delete(userRepository.findByLoginId(InitialDataLoader.ADVISOR_LOGIN_ID).orElseThrow());
		userRepository.flush();

		assertThat(loader.activeDemoAccountRoles()).containsExactly(Role.SEEKER);
	}

	// 警告は出すが、ログイン ID とパスワードはログに書かないこと。
	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void run_withActiveDemoAccounts_logsWarningWithoutCredentials(CapturedOutput output) {
		loader.run(new DefaultApplicationArguments());

		assertThat(output).contains("Demo accounts with passwords published in the README are active")
				.doesNotContain(InitialDataLoader.ADVISOR_LOGIN_ID, InitialDataLoader.ADVISOR_PASSWORD,
						InitialDataLoader.SEEKER_LOGIN_ID, InitialDataLoader.SEEKER_PASSWORD);
	}

	private void assertPasswordIsBcryptHashOf(AppUser user, String rawPassword) {
		assertThat(user.getPasswordHash()).isNotEqualTo(rawPassword).startsWith("$2");
		assertThat(passwordEncoder.matches(rawPassword, user.getPasswordHash())).isTrue();
	}

	// DB の応募が、定義した会社名と最終更新日時の組み合わせと一致すること（保存した時刻のままなら一致しない）。
	private void assertCompanyAndUpdatedAt(String loginId, List<DemoApplication> expected) {
		List<Tuple> expectedTuples = expected.stream()
				.map(demo -> tuple(demo.companyName(), demo.updatedAt()))
				.toList();

		assertThat(applicationsOf(userRepository.findByLoginId(loginId).orElseThrow()))
				.extracting(JobApplication::getCompanyName, JobApplication::getUpdatedAt)
				.containsExactlyInAnyOrderElementsOf(expectedTuples);
	}

	private List<AppUser> demoSeekers() {
		return DEMO_SEEKER_LOGIN_IDS.stream()
				.map(loginId -> userRepository.findByLoginId(loginId).orElseThrow())
				.toList();
	}

	private List<SeekerSummary> demoSeekerSummaries() {
		Set<Long> demoIds = demoSeekers().stream().map(AppUser::getId).collect(Collectors.toSet());
		return userRepository.findSeekerSummaries().stream()
				.filter(s -> demoIds.contains(s.userId()))
				.toList();
	}

	private List<JobApplication> applicationsOf(AppUser seeker) {
		return applicationRepository.findByOwnerIdOrderByAppliedDateDescIdDesc(seeker.getId());
	}

	private Map<String, List<Instant>> updatedAtByDemoLoginId() {
		Map<String, List<Instant>> result = new LinkedHashMap<>();
		for (AppUser seeker : demoSeekers()) {
			result.put(seeker.getLoginId(), applicationsOf(seeker).stream().map(JobApplication::getUpdatedAt).toList());
		}
		return result;
	}
}
