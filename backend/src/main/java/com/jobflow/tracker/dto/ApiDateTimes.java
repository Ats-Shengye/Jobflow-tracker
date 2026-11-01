package com.jobflow.tracker.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

// 日時は UTC の Instant で保存している。応答では日本時間の +09:00 付き（2026-10-30T18:30:00+09:00）にする。
final class ApiDateTimes {

	private static final ZoneId RESPONSE_ZONE = ZoneId.of("Asia/Tokyo");

	private ApiDateTimes() {
	}

	// 画面は分までしか出さないので、秒より下（保存はマイクロ秒まで）は切り捨てて例の形にそろえる。
	static OffsetDateTime toResponse(Instant instant) {
		if (instant == null) {
			return null;
		}
		return instant.truncatedTo(ChronoUnit.SECONDS).atZone(RESPONSE_ZONE).toOffsetDateTime();
	}
}
