package com.jobflow.tracker.dto;

import java.time.LocalDate;

import com.jobflow.tracker.entity.ApplicationStatus;
import com.jobflow.tracker.entity.JobApplication;

// 応募一覧（カード表示）の1件。給与・勤務地・メモ・最終更新日時は入れない。
public record ApplicationSummaryResponse(Long applicationId, String companyName, String position,
		ApplicationStatus status, Integer interviewRound, LocalDate appliedDate) {

	public static ApplicationSummaryResponse from(JobApplication application) {
		return new ApplicationSummaryResponse(application.getId(), application.getCompanyName(),
				application.getPosition(), application.getStatus(), application.getInterviewRound(),
				application.getAppliedDate());
	}

	@Override
	public String toString() {
		return "ApplicationSummaryResponse[applicationId=" + applicationId + ", status=" + status + "]";
	}
}
