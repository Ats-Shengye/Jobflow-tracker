package com.jobflow.tracker.service;

import java.io.Serial;

// 対象が無い時の例外（API では 404）。他人の応募も同じ例外にする。メッセージに ID を入れない。
public class ResourceNotFoundException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public ResourceNotFoundException() {
		super("resource not found");
	}
}
