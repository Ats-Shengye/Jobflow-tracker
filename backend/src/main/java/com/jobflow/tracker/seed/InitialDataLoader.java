package com.jobflow.tracker.seed;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.JobApplication;
import com.jobflow.tracker.entity.Role;
import com.jobflow.tracker.repository.AppUserRepository;
import com.jobflow.tracker.repository.JobApplicationRepository;
import com.jobflow.tracker.seed.DemoData.DemoApplication;
import com.jobflow.tracker.seed.DemoData.DemoSeeker;

// 起動時に担当者とデモ用の求職者（7人）を作る。同じログイン ID があれば作らないので、再起動しても二重にならない。
// デモの応募は求職者を新しく作った時だけ入れる（既にいる求職者の応募には手を付けない）。
@Component
public class InitialDataLoader implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(InitialDataLoader.class);

	// README に載せる練習用のアカウント。ローカルで動かす前提で、本番には使わない。氏名は架空のもの。
	static final String ADVISOR_LOGIN_ID = "advisor01";
	static final String ADVISOR_PASSWORD = "advisor-demo-2026";
	static final String ADVISOR_NAME = "田中 花子";

	static final String SEEKER_LOGIN_ID = "seeker01";
	static final String SEEKER_PASSWORD = "seeker-demo-2026";
	static final String SEEKER_NAME = "山田 太郎";

	// README に載せない求職者のパスワードの長さ。英数字32文字で約190ビットあり、パスワードの規則（15〜64文字）にも収まる。
	private static final int UNUSABLE_PASSWORD_LENGTH = 32;

	private static final String PASSWORD_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	private final AppUserRepository userRepository;
	private final JobApplicationRepository applicationRepository;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom = new SecureRandom();

	public InitialDataLoader(AppUserRepository userRepository, JobApplicationRepository applicationRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.applicationRepository = applicationRepository;
		this.passwordEncoder = passwordEncoder;
	}

	// 途中で失敗した時に中途半端なデータが残らないよう、まとめて1つのトランザクションにする。
	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		createUserIfAbsent(ADVISOR_LOGIN_ID, ADVISOR_PASSWORD, ADVISOR_NAME, Role.ADVISOR);
		createUserIfAbsent(SEEKER_LOGIN_ID, SEEKER_PASSWORD, SEEKER_NAME, Role.SEEKER)
				.ifPresent(seeker -> createDemoApplications(seeker, DemoData.MAIN_SEEKER_APPLICATIONS));
		for (DemoSeeker demo : DemoData.EXTRA_SEEKERS) {
			// 乱数のパスワードはハッシュにしたら捨てるので、このアカウントには誰もログインできない。
			createUserIfAbsent(demo.loginId(), unusablePassword(), demo.name(), Role.SEEKER)
					.ifPresent(seeker -> createDemoApplications(seeker, demo.applications()));
		}

		List<Role> activeDemoRoles = activeDemoAccountRoles();
		if (!activeDemoRoles.isEmpty()) {
			// public のリポジトリを土台に公開された時、固定パスワードのアカウントが残っていると気付けるように。ID とパスワードは書かない。
			log.warn("Demo accounts with passwords published in the README are active (roles={}). "
					+ "Do not use them in a publicly accessible environment.", activeDemoRoles);
		}
	}

	// アカウントがあり、README のパスワードでまだログインできるものを「有効」とみなす。
	List<Role> activeDemoAccountRoles() {
		List<Role> roles = new ArrayList<>();
		if (acceptsPassword(ADVISOR_LOGIN_ID, ADVISOR_PASSWORD)) {
			roles.add(Role.ADVISOR);
		}
		if (acceptsPassword(SEEKER_LOGIN_ID, SEEKER_PASSWORD)) {
			roles.add(Role.SEEKER);
		}
		return roles;
	}

	private boolean acceptsPassword(String loginId, String rawPassword) {
		return userRepository.findByLoginId(loginId)
				.map(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
				.orElse(false);
	}

	// 作った時だけユーザーを返す。既にあれば空。
	private Optional<AppUser> createUserIfAbsent(String loginId, String rawPassword, String name, Role role) {
		if (userRepository.existsByLoginId(loginId)) {
			log.debug("Initial {} account already exists; skipped", role);
			return Optional.empty();
		}
		AppUser created = userRepository.save(new AppUser(loginId, passwordEncoder.encode(rawPassword), name, role));
		// ログイン ID とパスワードはログに出さない。
		log.info("Created initial {} account (user_id={})", role, created.getId());
		return Optional.of(created);
	}

	// ログ・定数・テストのどこにも残さない使い捨てのパスワード。
	private String unusablePassword() {
		StringBuilder password = new StringBuilder(UNUSABLE_PASSWORD_LENGTH);
		for (int i = 0; i < UNUSABLE_PASSWORD_LENGTH; i++) {
			password.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
		}
		return password.toString();
	}

	// 保存の時に @PrePersist が最終更新日時を今の時刻にするので、全件保存した後で決めた日時に書き換える。
	// 書き換えの一括 update は永続化コンテキストを空にするため、保存を先に済ませておく。
	private void createDemoApplications(AppUser seeker, List<DemoApplication> demoApplications) {
		if (demoApplications.isEmpty()) {
			return;
		}
		List<JobApplication> saved = demoApplications.stream()
				.map(demo -> applicationRepository.save(demo.toEntity(seeker)))
				.toList();
		for (int i = 0; i < saved.size(); i++) {
			Long applicationId = saved.get(i).getId();
			int updatedRows = applicationRepository.overwriteUpdatedAtForSeedData(applicationId, seeker.getId(),
					demoApplications.get(i).updatedAt());
			if (updatedRows != 1) {
				throw new IllegalStateException(
						"Failed to set updated_at of a demo application (application_id=" + applicationId + ")");
			}
		}
		log.info("Created {} demo applications for a demo seeker (user_id={})", saved.size(), seeker.getId());
	}
}
