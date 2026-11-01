package com.jobflow.tracker.seed;

import java.time.LocalDate;
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
import com.jobflow.tracker.entity.ApplicationStatus;
import com.jobflow.tracker.entity.EmploymentInsurance;
import com.jobflow.tracker.entity.JobApplication;
import com.jobflow.tracker.entity.Role;
import com.jobflow.tracker.repository.AppUserRepository;
import com.jobflow.tracker.repository.JobApplicationRepository;

// 起動時に担当者とデモ用の求職者を作る。同じログイン ID があれば作らないので、再起動しても二重にならない。
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

	private final AppUserRepository userRepository;
	private final JobApplicationRepository applicationRepository;
	private final PasswordEncoder passwordEncoder;

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
				.ifPresent(this::createDemoApplications);
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

	// スクリーンショット用。全ステータスと、任意項目の入力あり・なしがそろうようにしている。
	// 会社名は全角・半角の英字と、㈱ と (株) の表示を見比べるためのもの。
	private void createDemoApplications(AppUser seeker) {
		List<JobApplication> applications = List.of(
				new JobApplication(seeker, "株式会社Ａ", "Webエンジニア（Java）", EmploymentInsurance.YES,
						LocalDate.of(2026, 10, 10), ApplicationStatus.INTERVIEW, 2, "年収350万〜450万円", "福岡市博多区",
						"二次面接は技術面接。ポートフォリオの説明を準備する"),
				new JobApplication(seeker, "B株式会社", "社内SE", EmploymentInsurance.UNKNOWN,
						LocalDate.of(2026, 10, 15), ApplicationStatus.DOCUMENT_SCREENING, null, null, null, null),
				new JobApplication(seeker, "㈱C", "データ入力", EmploymentInsurance.NO,
						LocalDate.of(2026, 10, 20), ApplicationStatus.APPLIED, null, "時給1,100円", null, null),
				new JobApplication(seeker, "D㈱", "営業事務", EmploymentInsurance.YES,
						LocalDate.of(2026, 9, 1), ApplicationStatus.OFFERED, null, "月給22万円", "佐賀市",
						"内定。入社日の希望を返信する"),
				new JobApplication(seeker, "合同会社Ｅ", "テストエンジニア", EmploymentInsurance.YES,
						LocalDate.of(2026, 9, 20), ApplicationStatus.REJECTED, null, null, "フルリモート",
						"書類選考で不採用。志望動機を見直す"),
				new JobApplication(seeker, "(株)F", "ヘルプデスク", EmploymentInsurance.YES,
						LocalDate.of(2026, 9, 25), ApplicationStatus.DECLINED, null, "月給20万円〜", "佐賀市", null));
		applications.forEach(applicationRepository::save);
		log.info("Created {} demo applications for the demo seeker (user_id={})", applications.size(),
				seeker.getId());
	}
}
