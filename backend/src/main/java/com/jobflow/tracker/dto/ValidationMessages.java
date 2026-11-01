package com.jobflow.tracker.dto;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.JobApplication;
import com.jobflow.tracker.security.CredentialRules;

// 入力チェックで返す文言（詳細設計の各画面の表に合わせる）。文字数は定数から組み立て、上限を変えた時に文言だけ古くならないようにする。
final class ValidationMessages {

	// ログイン画面は未入力の文言が1つだけなので、どちらの項目にも同じ文言を返す。
	static final String LOGIN_REQUIRED = "ログインIDとパスワードを入力してください";

	static final String LOGIN_ID_REQUIRED = "ログインIDを入力してください";

	static final String LOGIN_ID_FORMAT = "ログインIDは半角英数字と記号（空白なし）" + AppUser.LOGIN_ID_MIN_LENGTH + "〜"
			+ AppUser.LOGIN_ID_MAX_LENGTH + "文字で入力してください";

	static final String PASSWORD_REQUIRED = "パスワードを入力してください";

	static final String PASSWORD_FORMAT = "パスワードは半角英数字・記号・半角スペースで" + CredentialRules.PASSWORD_MIN_LENGTH + "〜"
			+ CredentialRules.PASSWORD_MAX_LENGTH + "文字で入力してください";

	static final String NAME_REQUIRED = "氏名を入力してください";

	static final String NAME_TOO_LONG = "氏名は" + AppUser.NAME_MAX_LENGTH + "文字以内で入力してください";

	static final String NAME_INVALID_CHARACTER = "氏名に使用できない文字が含まれています";

	static final String COMPANY_NAME_REQUIRED = "企業名を入力してください";

	static final String COMPANY_NAME_TOO_LONG = "企業名は" + JobApplication.TITLE_MAX_LENGTH + "文字以内で入力してください";

	static final String POSITION_REQUIRED = "ポジションを入力してください";

	static final String POSITION_TOO_LONG = "ポジションは" + JobApplication.TITLE_MAX_LENGTH + "文字以内で入力してください";

	static final String EMPLOYMENT_INSURANCE_REQUIRED = "雇用保険の有無を入力してください";

	static final String APPLIED_DATE_REQUIRED = "申込日を入力してください";

	static final String STATUS_REQUIRED = "進捗ステータスを入力してください";

	// 面接回数は未入力も範囲外も同じ文言にする（詳細設計の応募登録画面の表）。
	static final String INTERVIEW_ROUND_INVALID = "面接回数は" + JobApplication.MIN_INTERVIEW_ROUND + "〜"
			+ JobApplication.MAX_INTERVIEW_ROUND + "で選択してください";

	static final String SALARY_TOO_LONG = "給与は" + JobApplication.FREE_TEXT_MAX_LENGTH + "文字以内で入力してください";

	static final String WORK_LOCATION_TOO_LONG = "勤務地は" + JobApplication.FREE_TEXT_MAX_LENGTH + "文字以内で入力してください";

	static final String MEMO_TOO_LONG = "メモは" + JobApplication.MEMO_MAX_LENGTH + "文字以内で入力してください";

	private ValidationMessages() {
	}
}
