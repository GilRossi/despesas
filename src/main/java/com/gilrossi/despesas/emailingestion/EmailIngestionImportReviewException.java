package com.gilrossi.despesas.emailingestion;

public class EmailIngestionImportReviewException extends RuntimeException {

	private final EmailIngestionDecisionReason reason;

	public EmailIngestionImportReviewException(EmailIngestionDecisionReason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public EmailIngestionDecisionReason getReason() {
		return reason;
	}
}
