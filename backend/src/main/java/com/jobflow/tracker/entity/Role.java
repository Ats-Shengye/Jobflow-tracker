package com.jobflow.tracker.entity;

// DB と API には英字コードのまま入れ、日本語の表示名は画面側で変換する。
public enum Role {

	SEEKER, // 求職者

	ADVISOR // 担当者（初期データでだけ作る）
}
