package com.jobflow.tracker.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.jobflow.tracker.entity.ApplicationStatus;
import com.jobflow.tracker.entity.EmploymentInsurance;
import com.jobflow.tracker.entity.JobApplication;
import com.jobflow.tracker.service.ApplicationContent;

// 応募登録・応募更新の本文（更新も全項目の置き換えなので同じ形）。
// user_id は項目に持たないので、本文に入っていても読まれない。持ち主はログイン中の利用者で決める。
@ValidInterviewRound
public record ApplicationRequest(
		@NotBlank(message = ValidationMessages.COMPANY_NAME_REQUIRED)
		@Size(max = JobApplication.TITLE_MAX_LENGTH, message = ValidationMessages.COMPANY_NAME_TOO_LONG)
		String companyName,

		@NotBlank(message = ValidationMessages.POSITION_REQUIRED)
		@Size(max = JobApplication.TITLE_MAX_LENGTH, message = ValidationMessages.POSITION_TOO_LONG)
		String position,

		@NotNull(message = ValidationMessages.EMPLOYMENT_INSURANCE_REQUIRED)
		EmploymentInsurance employmentInsurance,

		@NotNull(message = ValidationMessages.APPLIED_DATE_REQUIRED)
		LocalDate appliedDate,

		@NotNull(message = ValidationMessages.STATUS_REQUIRED)
		ApplicationStatus status,

		// 面接の時だけ必須で 1〜5。確かめるのはクラスに付けた @ValidInterviewRound。
		Integer interviewRound,

		@Size(max = JobApplication.FREE_TEXT_MAX_LENGTH, message = ValidationMessages.SALARY_TOO_LONG)
		String salary,

		@Size(max = JobApplication.FREE_TEXT_MAX_LENGTH, message = ValidationMessages.WORK_LOCATION_TOO_LONG)
		String workLocation,

		@Size(max = JobApplication.MEMO_MAX_LENGTH, message = ValidationMessages.MEMO_TOO_LONG)
		String memo) {

	// 文字数は氏名と同じく、前後の空白（全角スペースを含む）を削ってから数える。
	// 任意項目は空文字・空白だけなら null にする。面接以外の時に送られてきた回数はエラーにせず捨てる。
	public ApplicationRequest {
		companyName = strip(companyName);
		position = strip(position);
		salary = blankToNull(salary);
		workLocation = blankToNull(workLocation);
		memo = blankToNull(memo);
		if (status != ApplicationStatus.INTERVIEW) {
			interviewRound = null;
		}
	}

	public ApplicationContent toContent() {
		return new ApplicationContent(companyName, position, employmentInsurance, appliedDate, status,
				interviewRound, salary, workLocation, memo);
	}

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}

	private static String blankToNull(String value) {
		String stripped = strip(value);
		return stripped == null || stripped.isEmpty() ? null : stripped;
	}

	// 企業名・メモなどの中身がログに混ざらないよう伏せる。
	@Override
	public String toString() {
		return "ApplicationRequest[status=" + status + ", interviewRound=" + interviewRound + ", other fields=***]";
	}
}
