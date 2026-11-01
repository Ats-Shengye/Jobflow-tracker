package com.jobflow.tracker.seed;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.ApplicationStatus;
import com.jobflow.tracker.entity.EmploymentInsurance;
import com.jobflow.tracker.entity.JobApplication;

// 初期データのデモの求職者と応募（スクリーンショット用）。氏名・会社名はすべて架空。
// 最終更新日時は日本時間で書き、保存する時に UTC の Instant に直す。
final class DemoData {

	static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");

	private static final int YEAR = 2026;

	private DemoData() {
	}

	record DemoApplication(String companyName, String position, EmploymentInsurance employmentInsurance,
			LocalDate appliedDate, ApplicationStatus status, Integer interviewRound, String salary,
			String workLocation, String memo, LocalDateTime updatedAtInJapan) {

		// 夜中の更新ばかりだと一覧が不自然に見えるので、日本時間の日中に限る。
		private static final LocalTime DAYTIME_START = LocalTime.of(9, 0);

		private static final LocalTime DAYTIME_END = LocalTime.of(20, 0);

		DemoApplication {
			Objects.requireNonNull(appliedDate, "appliedDate must not be null");
			Objects.requireNonNull(updatedAtInJapan, "updatedAtInJapan must not be null");
			if (updatedAtInJapan.toLocalDate().isBefore(appliedDate)) {
				throw new IllegalArgumentException("updatedAtInJapan must not be before appliedDate");
			}
			LocalTime time = updatedAtInJapan.toLocalTime();
			if (time.isBefore(DAYTIME_START) || time.isAfter(DAYTIME_END)) {
				throw new IllegalArgumentException("updatedAtInJapan must be between 09:00 and 20:00");
			}
		}

		JobApplication toEntity(AppUser owner) {
			return new JobApplication(owner, companyName, position, employmentInsurance, appliedDate, status,
					interviewRound, salary, workLocation, memo);
		}

		Instant updatedAt() {
			return updatedAtInJapan.atZone(JAPAN).toInstant();
		}
	}

	record DemoSeeker(String loginId, String name, List<DemoApplication> applications) {
	}

	// seeker01 の応募。全ステータスと、任意項目の入力あり・なしがそろうようにしている。
	// 会社名は全角・半角の英字と、㈱ と (株) の表示を見比べるためのもの。最終更新日時は7人の中で一番新しくする。
	static final List<DemoApplication> MAIN_SEEKER_APPLICATIONS = List.of(
			new DemoApplication("株式会社Ａ", "Webエンジニア（Java）", EmploymentInsurance.YES, date(10, 10),
					ApplicationStatus.INTERVIEW, 2, "年収350万〜450万円", "福岡市博多区",
					"二次面接は技術面接。ポートフォリオの説明を準備する", at(10, 30, 15, 20)),
			new DemoApplication("B株式会社", "社内SE", EmploymentInsurance.UNKNOWN, date(10, 15),
					ApplicationStatus.DOCUMENT_SCREENING, null, null, null, null, at(10, 16, 11, 5)),
			new DemoApplication("㈱C", "データ入力", EmploymentInsurance.NO, date(10, 20),
					ApplicationStatus.APPLIED, null, "時給1,100円", null, null, at(10, 30, 10, 40)),
			new DemoApplication("D㈱", "営業事務", EmploymentInsurance.YES, date(9, 1),
					ApplicationStatus.OFFERED, null, "月給22万円", "佐賀市", "内定。入社日の希望を返信する",
					at(10, 12, 17, 30)),
			new DemoApplication("合同会社Ｅ", "テストエンジニア", EmploymentInsurance.YES, date(9, 20),
					ApplicationStatus.REJECTED, null, null, "フルリモート", "書類選考で不採用。志望動機を見直す",
					at(9, 26, 13, 10)),
			new DemoApplication("(株)F", "ヘルプデスク", EmploymentInsurance.YES, date(9, 25),
					ApplicationStatus.DECLINED, null, "月給20万円〜", "佐賀市", null, at(10, 2, 18, 45)));

	// README に載せない求職者。同じ字・同じ読みの氏名で管理No（user_id）が要る場面を見せ、
	// 最新の最終更新日を人ごとにずらして古い順の並べ替えが分かるようにする。0件（最終更新日が空）の人は2人。
	static final List<DemoSeeker> EXTRA_SEEKERS = List.of(
			new DemoSeeker("seeker02", "山田 太朗", List.of(
					new DemoApplication("H商事株式会社", "営業事務", EmploymentInsurance.YES, date(8, 22),
							ApplicationStatus.REJECTED, null, "月給19万円", "久留米市", "書類選考で不採用",
							at(9, 3, 14, 30)),
					new DemoApplication("株式会社Ｇ", "プログラマー（Java）", EmploymentInsurance.UNKNOWN, date(9, 3),
							ApplicationStatus.INTERVIEW, 1, null, "福岡市中央区", null, at(9, 18, 16, 10)))),
			new DemoSeeker("seeker03", "山田 汰朗", List.of()),
			new DemoSeeker("seeker04", "山田 太郎", List.of(
					new DemoApplication("J工業株式会社", "製造オペレーター", EmploymentInsurance.YES, date(7, 12),
							ApplicationStatus.APPLIED, null, "時給1,150円", "鳥栖市", null, at(7, 17, 10, 20)))),
			new DemoSeeker("seeker05", "佐藤 一郎", List.of(
					new DemoApplication("K㈱", "経理事務", EmploymentInsurance.YES, date(8, 4),
							ApplicationStatus.DECLINED, null, "月給22万円〜", "佐賀市", "他社の内定を承諾したため辞退",
							at(9, 12, 11, 45)),
					new DemoApplication("合同会社L", "ITサポート", EmploymentInsurance.UNKNOWN, date(9, 12),
							ApplicationStatus.DOCUMENT_SCREENING, null, null, null, null, at(9, 26, 9, 30)),
					new DemoApplication("株式会社Ｍ", "カスタマーサポート", EmploymentInsurance.YES, date(9, 18),
							ApplicationStatus.INTERVIEW, 3, "年収300万〜350万円", "フルリモート", "最終面接の日程を調整中",
							at(10, 12, 19, 15)),
					new DemoApplication("N株式会社", "総務", EmploymentInsurance.NO, date(10, 2),
							ApplicationStatus.OFFERED, null, "月給20万円", "佐賀市", null, at(10, 10, 13, 0)))),
			new DemoSeeker("seeker06", "佐藤 一朗", List.of(
					new DemoApplication("(株)P", "倉庫作業スタッフ", EmploymentInsurance.NO, date(7, 13),
							ApplicationStatus.REJECTED, null, "時給1,050円", null, null, at(7, 25, 15, 40)),
					new DemoApplication("株式会社Ｑ", "品質管理", EmploymentInsurance.YES, date(7, 25),
							ApplicationStatus.INTERVIEW, 2, null, "鳥栖市", "二次面接の結果待ち", at(8, 24, 12, 5)),
					new DemoApplication("R販売株式会社", "販売スタッフ", EmploymentInsurance.YES, date(8, 10),
							ApplicationStatus.APPLIED, null, null, null, null, at(8, 15, 17, 50)))),
			new DemoSeeker("seeker07", "佐藤 壱朗", List.of()));

	private static LocalDate date(int month, int day) {
		return LocalDate.of(YEAR, month, day);
	}

	private static LocalDateTime at(int month, int day, int hour, int minute) {
		return LocalDateTime.of(YEAR, month, day, hour, minute);
	}
}
