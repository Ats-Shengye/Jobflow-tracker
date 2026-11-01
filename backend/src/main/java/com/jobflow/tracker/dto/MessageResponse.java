package com.jobflow.tracker.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

// エラーとログアウトの応答の本文。errors は 400 の入力不備の時だけ入れ、それ以外は JSON に出さない。
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageResponse(String message, Map<String, String> errors) {

	public static MessageResponse of(String message) {
		return new MessageResponse(message, null);
	}
}
