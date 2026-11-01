package com.jobflow.tracker.dto;

import java.time.OffsetDateTime;

import com.jobflow.tracker.repository.SeekerSummary;

// 担当者用の求職者一覧の1件。応募が0件なら last_updated_at は null。
public record SeekerSummaryResponse(Long userId, String name, long applicationCount, OffsetDateTime lastUpdatedAt) {

	public static SeekerSummaryResponse from(SeekerSummary summary) {
		return new SeekerSummaryResponse(summary.userId(), summary.name(), summary.applicationCount(),
				ApiDateTimes.toResponse(summary.lastUpdatedAt()));
	}

	@Override
	public String toString() {
		return "SeekerSummaryResponse[userId=" + userId + ", applicationCount=" + applicationCount + "]";
	}
}
