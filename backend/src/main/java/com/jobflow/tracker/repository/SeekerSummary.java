package com.jobflow.tracker.repository;

import java.time.Instant;

// 担当者用の求職者一覧で使う集計。応募が0件の時、lastUpdatedAt は null になる。
// JPQL のコンストラクタ式で作るので、型は select の値に合わせる（count は Long）。
public record SeekerSummary(Long userId, String name, Long applicationCount, Instant lastUpdatedAt) {
}
