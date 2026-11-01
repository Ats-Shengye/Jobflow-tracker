package com.jobflow.tracker.service;

import java.time.LocalDate;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.ApplicationStatus;
import com.jobflow.tracker.entity.EmploymentInsurance;
import com.jobflow.tracker.entity.JobApplication;

// 応募の登録・更新で書き込む値。入力チェックは API の DTO で済ませてから渡す。持ち主はここに含めない。
public record ApplicationContent(String companyName, String position, EmploymentInsurance employmentInsurance,
		LocalDate appliedDate, ApplicationStatus status, Integer interviewRound, String salary, String workLocation,
		String memo) {

	JobApplication newApplication(AppUser owner) {
		return new JobApplication(owner, companyName, position, employmentInsurance, appliedDate, status,
				interviewRound, salary, workLocation, memo);
	}

	void applyTo(JobApplication application) {
		application.update(companyName, position, employmentInsurance, appliedDate, status, interviewRound, salary,
				workLocation, memo);
	}

	// 企業名・メモなどの中身がログに混ざらないよう、ステータスだけにする。
	@Override
	public String toString() {
		return "ApplicationContent[status=" + status + ", other fields=***]";
	}
}
