package com.gilrossi.despesas.security;

public record OperationalRequestVerificationResult(
	String keyId,
	String nonceFingerprint,
	String bodyHashPrefix,
	String sourceAccount
) {
}
