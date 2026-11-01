package com.jobflow.tracker.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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

// 起動時に一度シードが走った状態を確かめ、もう一度 run して二重にならないことを見る。
// JobflowTrackerApplicationTests と同じ設定（素の @SpringBootTest）のままにすること。テスト用の H2 は JVM の中で
// 共有されるので、設定の違うコンテキストが増えると create-drop で先のコンテキストのテーブルが消される。
@SpringBootTest
class InitialDataLoaderTests {

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

	// スクリーンショット用なので、全ステータスと、回数の入った面接がそろっていること。
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
	}

	// 再起動のつもりでもう一度 run しても、アカウントも応募も増えないこと。
	@Test
	void run_whenDataAlreadyExists_createsNothing() {
		long userCountBefore = userRepository.count();
		AppUser seeker = userRepository.findByLoginId(InitialDataLoader.SEEKER_LOGIN_ID).orElseThrow();
		long demoApplicationsBefore = applicationRepository.countByOwnerId(seeker.getId());

		loader.run(new DefaultApplicationArguments());

		assertThat(userRepository.count()).isEqualTo(userCountBefore);
		assertThat(applicationRepository.countByOwnerId(seeker.getId())).isEqualTo(demoApplicationsBefore);
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
}
