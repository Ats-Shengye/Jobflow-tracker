package com.jobflow.tracker.service;

import java.io.Serial;

// メッセージにログイン ID を入れない（ログに残るため）。
public class DuplicateLoginIdException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public DuplicateLoginIdException() {
		super("login_id is already in use");
	}
}
