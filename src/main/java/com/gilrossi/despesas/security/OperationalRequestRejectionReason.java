package com.gilrossi.despesas.security;

import org.springframework.http.HttpStatus;

public enum OperationalRequestRejectionReason {

	MISSING_REQUIRED_HEADER(HttpStatus.UNAUTHORIZED),
	INVALID_TIMESTAMP(HttpStatus.UNAUTHORIZED),
	TIMESTAMP_OUTSIDE_WINDOW(HttpStatus.UNAUTHORIZED),
	INVALID_KEY_ID(HttpStatus.UNAUTHORIZED),
	INVALID_BODY_HASH(HttpStatus.UNAUTHORIZED),
	INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED),
	REPLAY_DETECTED(HttpStatus.UNAUTHORIZED),
	INVALID_JSON(HttpStatus.BAD_REQUEST),
	FORBIDDEN_HOUSEHOLD_FIELD(HttpStatus.UNPROCESSABLE_ENTITY);

	private final HttpStatus httpStatus;

	OperationalRequestRejectionReason(HttpStatus httpStatus) {
		this.httpStatus = httpStatus;
	}

	public HttpStatus httpStatus() {
		return httpStatus;
	}
}
