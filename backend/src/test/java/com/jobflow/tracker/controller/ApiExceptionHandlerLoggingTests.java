package com.jobflow.tracker.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;

// ログの出方は Spring Boot のログ設定（既定は INFO）に左右されるので、他の API テストと同じコンテキストで確かめる。
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerLoggingTests {

	@Autowired
	private ApiExceptionHandler handler;

	// 例外のメッセージには DB の値（メモなど）が入りうるので、原因の例外も含めてログに出さない。種類と発生箇所は出す。
	@Test
	void handleOther_unexpectedException_logsTypeAndLocationButNotMessage(CapturedOutput output) {
		IllegalStateException exception = new IllegalStateException("outer-marker-7f3a",
				new RuntimeException("inner-marker-9c1e"));

		assertThat(handler.handleOther(exception).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

		assertThat(output).contains("Unhandled exception: java.lang.IllegalStateException")
				.contains("at com.jobflow.tracker.controller.ApiExceptionHandlerLoggingTests")
				.doesNotContain("outer-marker-7f3a", "inner-marker-9c1e");
	}
}
