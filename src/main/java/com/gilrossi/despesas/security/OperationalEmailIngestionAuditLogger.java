package com.gilrossi.despesas.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision;

@Component
public class OperationalEmailIngestionAuditLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(OperationalEmailIngestionAuditLogger.class);

	public void requestReceived(String path, String keyId, String sourceAccount, String nonceFingerprint, String bodyHashPrefix) {
		LOGGER.info(
			"event=operational_request_received path={} keyId={} sourceAccount={} nonce={} bodyHash={}",
			normalize(path),
			normalize(keyId),
			maskAccount(sourceAccount),
			normalize(nonceFingerprint),
			normalize(bodyHashPrefix)
		);
	}

	public void requestRejected(String path, OperationalRequestValidationException exception) {
		LOGGER.warn(
			"event=operational_request_rejected path={} keyId={} sourceAccount={} nonce={} bodyHash={} reason={}",
			normalize(path),
			normalize(exception.getKeyId()),
			maskAccount(exception.getSourceAccount()),
			normalize(exception.getNonceFingerprint()),
			normalize(exception.getBodyHashPrefix()),
			exception.getReason().name().toLowerCase()
		);
	}

	public void sourceAccountRejected(String sourceAccount, String reason) {
		LOGGER.warn(
			"event=operational_ingestion_source_rejected sourceAccount={} reason={}",
			maskAccount(sourceAccount),
			normalize(reason)
		);
	}

	public void ingestionAccepted(
		String sourceAccount,
		Long householdId,
		Long ingestionId,
		EmailIngestionFinalDecision decision,
		boolean duplicate
	) {
		LOGGER.info(
			"event=operational_ingestion_accepted sourceAccount={} householdId={} ingestionId={} decision={} duplicate={}",
			maskAccount(sourceAccount),
			householdId,
			ingestionId,
			decision,
			duplicate
		);
	}

	private String maskAccount(String sourceAccount) {
		if (sourceAccount == null || sourceAccount.isBlank()) {
			return "-";
		}
		int atIndex = sourceAccount.indexOf('@');
		if (atIndex <= 1) {
			return "***";
		}
		return sourceAccount.charAt(0) + "***" + sourceAccount.substring(atIndex);
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}
}
