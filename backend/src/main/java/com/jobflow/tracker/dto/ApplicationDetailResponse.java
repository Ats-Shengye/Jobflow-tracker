package com.jobflow.tracker.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.jobflow.tracker.entity.ApplicationStatus;
import com.jobflow.tracker.entity.EmploymentInsurance;
import com.jobflow.tracker.entity.JobApplication;

// 応募詳細・登録・更新と、担当者の求職者別応募取得で返す全項目。持ち主の user_id は返さない。
public record ApplicationDetailResponse(Long applicationId, String companyName, String position,
		EmploymentInsurance employmentInsurance, LocalDate appliedDate, ApplicationStatus status,
		Integer interviewRound, String salary, String workLocation, String memo, OffsetDateTime updatedAt) {

	public static ApplicationDetailResponse from(JobApplication application) {
		return new ApplicationDetailResponse(application.getId(), application.getCompanyName(),
				application.getPosition(), application.getEmploymentInsurance(), application.getAppliedDate(),
				application.getStatus(), application.getInterviewRound(), application.getSalary(),
				application.getWorkLocation(), application.getMemo(),
				ApiDateTimes.toResponse(application.getUpdatedAt()));
	}

	// メモなどの中身がログに混ざらないよう、ID とステータスだけにする。
	@Override
	public String toString() {
		return "ApplicationDetailResponse[applicationId=" + applicationId + ", status=" + status + "]";
	}
}
