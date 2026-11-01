package com.jobflow.tracker.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

// 進捗ステータスが面接の時だけ、面接回数が 1〜5 で入っているかを確かめる。
// status と見比べるので項目ではなくクラスに付け、エラーは interview_round の項目に付ける。
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = InterviewRoundValidator.class)
public @interface ValidInterviewRound {

	String message() default ValidationMessages.INTERVIEW_ROUND_INVALID;

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
