package com.jobflow.tracker.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobflow.tracker.dto.SeekerApplicationsResponse;
import com.jobflow.tracker.dto.SeekerSummaryResponse;
import com.jobflow.tracker.security.LoginUser;
import com.jobflow.tracker.service.SeekerQueryService;

// 担当者用の API。担当者だけが呼べることは SecurityConfig で確かめる。閲覧のみなので GET しか置かない。
@RestController
@RequestMapping("/api/admin/seekers")
public class AdvisorController {

	private final SeekerQueryService seekerQueryService;

	public AdvisorController(SeekerQueryService seekerQueryService) {
		this.seekerQueryService = seekerQueryService;
	}

	@GetMapping
	public List<SeekerSummaryResponse> listSeekers() {
		return seekerQueryService.listSeekers().stream().map(SeekerSummaryResponse::from).toList();
	}

	// 誰が誰の応募を見たかをログに残すため、担当者の user_id も渡す。
	@GetMapping("/{id}/applications")
	public SeekerApplicationsResponse seekerApplications(@PathVariable("id") Long id,
			@AuthenticationPrincipal LoginUser user) {
		return SeekerApplicationsResponse
				.from(seekerQueryService.getSeekerApplications(CurrentUser.requireId(user), id));
	}
}
