package com.jobflow.tracker.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.jobflow.tracker.dto.MessageResponse;

// 想定外の例外は API からは起こしにくいので、ハンドラーを直接呼んで確かめる。
class ApiExceptionHandlerTests {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	// 例外のメッセージに SQL や入力値が入っていても、応答には汎用の文言だけを返す。
	@Test
	void handleOther_unexpectedException_returns500WithoutInternalDetails() {
		ResponseEntity<MessageResponse> response = handler
				.handleOther(new IllegalStateException("SELECT password FROM users WHERE login_id='secret01'"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().message()).isEqualTo(ApiExceptionHandler.SERVER_ERROR_MESSAGE)
				.doesNotContain("SELECT").doesNotContain("secret01");
		assertThat(response.getBody().errors()).isNull();
	}

	// 入力エラーのキーを、JSON の他の項目と同じ snake_case にする。
	@ParameterizedTest
	@CsvSource({ "loginId, login_id", "interviewRound, interview_round", "name, name" })
	void toSnakeCase_convertsDtoFieldNameToJsonKey(String fieldName, String expected) {
		assertThat(ApiExceptionHandler.toSnakeCase(fieldName)).isEqualTo(expected);
	}

	// ライブラリの中だけで起きた例外は、発生箇所を unknown にする（他のパッケージの位置を取り違えない）。
	@Test
	void firstApplicationFrame_withoutApplicationFrames_returnsUnknown() {
		RuntimeException exception = withStackTrace(new RuntimeException("x"), "org.hibernate.SomeClass");

		assertThat(ApiExceptionHandler.firstApplicationFrame(exception)).isEqualTo("unknown");
	}

	// Spring が包み直した例外は外側にアプリのフレームが無いことがあるので、cause の側から探す。
	@Test
	void firstApplicationFrame_onlyInCause_returnsFrameFromCause() {
		RuntimeException cause = withStackTrace(new RuntimeException("inner"),
				"com.jobflow.tracker.service.UserRegistrationService");
		RuntimeException wrapper = withStackTrace(new RuntimeException("outer", cause),
				"org.springframework.SomeProxy");

		assertThat(ApiExceptionHandler.firstApplicationFrame(wrapper))
				.isEqualTo("com.jobflow.tracker.service.UserRegistrationService.registerSeeker:42");
	}

	private static RuntimeException withStackTrace(RuntimeException exception, String className) {
		exception.setStackTrace(new StackTraceElement[] {
				new StackTraceElement(className, "registerSeeker", "Source.java", 42) });
		return exception;
	}
}
