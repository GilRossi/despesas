package com.gilrossi.despesas.security;

public class OperationalRequestValidationException extends RuntimeException {

	private final OperationalRequestRejectionReason reason;
	private final String keyId;
	private final String nonceFingerprint;
	private final String bodyHashPrefix;
	private final String sourceAccount;

	public OperationalRequestValidationException(
		OperationalRequestRejectionReason reason,
		String keyId,
		String nonceFingerprint,
		String bodyHashPrefix,
		String sourceAccount
	) {
		super(reason.name());
		this.reason = reason;
		this.keyId = keyId;
		this.nonceFingerprint = nonceFingerprint;
		this.bodyHashPrefix = bodyHashPrefix;
		this.sourceAccount = sourceAccount;
	}

	public OperationalRequestRejectionReason getReason() {
		return reason;
	}

	public String getKeyId() {
		return keyId;
	}

	public String getNonceFingerprint() {
		return nonceFingerprint;
	}

	public String getBodyHashPrefix() {
		return bodyHashPrefix;
	}

	public String getSourceAccount() {
		return sourceAccount;
	}
}
