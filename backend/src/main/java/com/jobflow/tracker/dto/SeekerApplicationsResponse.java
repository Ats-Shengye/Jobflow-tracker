package com.jobflow.tracker.dto;

import java.util.List;

import com.jobflow.tracker.service.SeekerQueryService.SeekerApplications;

// 担当者の求職者別応募取得。応募は詳細と同じ全項目で、並びは応募一覧と同じ（申込日の降順）。
public record SeekerApplicationsResponse(Long userId, String name, List<ApplicationDetailResponse> applications) {

	public static SeekerApplicationsResponse from(SeekerApplications result) {
		return new SeekerApplicationsResponse(result.seeker().getId(), result.seeker().getName(),
				result.applications().stream().map(ApplicationDetailResponse::from).toList());
	}

	@Override
	public String toString() {
		return "SeekerApplicationsResponse[userId=" + userId + ", applications=" + applications.size() + "]";
	}
}
