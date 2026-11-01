package com.jobflow.tracker.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.jobflow.tracker.entity.ApplicationStatus;
import com.jobflow.tracker.entity.JobApplication;

// 面接以外の時の回数は ApplicationRequest のコンストラクタで null にしてあるので、ここでは面接の時だけを見る。
public class InterviewRoundValidator implements ConstraintValidator<ValidInterviewRound, ApplicationRequest> {

	@Override
	public boolean isValid(ApplicationRequest request, ConstraintValidatorContext context) {
		if (request == null || request.status() != ApplicationStatus.INTERVIEW) {
			return true;
		}
		Integer round = request.interviewRound();
		if (round != null && round >= JobApplication.MIN_INTERVIEW_ROUND
				&& round <= JobApplication.MAX_INTERVIEW_ROUND) {
			return true;
		}
		// 既定のままだと本文全体のエラーになり、errors のキーが付かないので、interview_round の項目に付け替える。
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
				.addPropertyNode("interviewRound")
				.addConstraintViolation();
		return false;
	}
}
