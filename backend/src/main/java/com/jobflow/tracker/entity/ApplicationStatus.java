package com.jobflow.tracker.entity;

// 面接は1つにまとめ、何次面接かは interview_round で持つ。押し間違いを戻せるよう遷移は制限しない。
public enum ApplicationStatus {

	APPLIED, // 応募済

	DOCUMENT_SCREENING, // 書類選考中

	INTERVIEW, // 面接

	OFFERED, // 内定

	REJECTED, // 不採用

	DECLINED // 辞退
}
