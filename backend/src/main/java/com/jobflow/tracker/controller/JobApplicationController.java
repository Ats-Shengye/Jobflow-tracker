package com.jobflow.tracker.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jobflow.tracker.dto.ApplicationDetailResponse;
import com.jobflow.tracker.dto.ApplicationRequest;
import com.jobflow.tracker.dto.ApplicationSummaryResponse;
import com.jobflow.tracker.dto.MessageResponse;
import com.jobflow.tracker.security.LoginUser;
import com.jobflow.tracker.service.JobApplicationService;

// 求職者の応募の API。求職者だけが呼べることは SecurityConfig で、持ち主であることはサービスで確かめる。
// パスの ID を先の引数に置き、数値でない ID は本文を読む前に 404 にする（ApiExceptionHandler）。
@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

	static final String DELETED_MESSAGE = "削除しました";

	private final JobApplicationService applicationService;

	public JobApplicationController(JobApplicationService applicationService) {
		this.applicationService = applicationService;
	}

	@GetMapping
	public List<ApplicationSummaryResponse> list(@AuthenticationPrincipal LoginUser user) {
		return applicationService.listOwn(CurrentUser.requireId(user)).stream()
				.map(ApplicationSummaryResponse::from)
				.toList();
	}

	@GetMapping("/{id}")
	public ApplicationDetailResponse get(@PathVariable("id") Long id, @AuthenticationPrincipal LoginUser user) {
		return ApplicationDetailResponse.from(applicationService.getOwn(CurrentUser.requireId(user), id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApplicationDetailResponse create(@Valid @RequestBody ApplicationRequest body,
			@AuthenticationPrincipal LoginUser user) {
		return ApplicationDetailResponse.from(applicationService.create(CurrentUser.requireId(user), body.toContent()));
	}

	@PutMapping("/{id}")
	public ApplicationDetailResponse update(@PathVariable("id") Long id, @Valid @RequestBody ApplicationRequest body,
			@AuthenticationPrincipal LoginUser user) {
		return ApplicationDetailResponse
				.from(applicationService.update(CurrentUser.requireId(user), id, body.toContent()));
	}

	@DeleteMapping("/{id}")
	public MessageResponse delete(@PathVariable("id") Long id, @AuthenticationPrincipal LoginUser user) {
		applicationService.delete(CurrentUser.requireId(user), id);
		return MessageResponse.of(DELETED_MESSAGE);
	}
}
