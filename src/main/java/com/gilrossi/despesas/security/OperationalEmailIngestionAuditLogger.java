package com.gilrossi.despesas.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.gilrossi.despesas.audit.PersistedAuditEventCategory;
import com.gilrossi.despesas.audit.PersistedAuditEventCommand;
import com.gilrossi.despesas.audit.PersistedAuditEventRecorder;
import com.gilrossi.despesas.audit.PersistedAuditEventStatus;
import com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision;
import com.gilrossi.despesas.ratelimit.RateLimitExceededException;

@Component
public class OperationalEmailIngestionAuditLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(OperationalEmailIngestionAuditLogger.class);
	private final PersistedAuditEventRecorder auditEventRecorder;

	@Autowired
	public OperationalEmailIngestionAuditLogger(ObjectProvider<PersistedAuditEventRecorder> auditEventRecorderProvider) {
		this(auditEventRecorderProvider.getIfAvailable());
	}

	public OperationalEmailIngestionAuditLogger() {
		this((PersistedAuditEventRecorder) null);
	}

	OperationalEmailIngestionAuditLogger(PersistedAuditEventRecorder auditEventRecorder) {
		this.auditEventRecorder = auditEventRecorder;
	}

	public void requestReceived(String path, String keyId, String sourceAccount, String nonceFingerprint, String bodyHashPrefix) {
		LOGGER.info(
			"event=operational_request_received path={} keyId={} sourceAccount={} nonce={} bodyHash={}",
			normalize(path),
			normalize(keyId),
			maskAccount(sourceAccount),
			normalize(nonceFingerprint),
			normalize(bodyHashPrefix)
		);
		record(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.OPERATIONAL, "operational_request_received", PersistedAuditEventStatus.STARTED)
			.sourceKey(normalize(keyId))
			.requestPath(normalize(path))
			.primaryReference(maskAccount(sourceAccount))
			.safeContext("nonceFingerprint", normalize(nonceFingerprint))
			.safeContext("bodyHashPrefix", normalize(bodyHashPrefix))
			.build());
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
		record(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.OPERATIONAL, "operational_request_rejected", PersistedAuditEventStatus.REJECTED)
			.sourceKey(normalize(exception.getKeyId()))
			.requestPath(normalize(path))
			.primaryReference(maskAccount(exception.getSourceAccount()))
			.detailCode(exception.getReason().name())
			.safeContext("nonceFingerprint", normalize(exception.getNonceFingerprint()))
			.safeContext("bodyHashPrefix", normalize(exception.getBodyHashPrefix()))
			.build());
	}

	public void sourceAccountRejected(String sourceAccount, String reason) {
		LOGGER.warn(
			"event=operational_ingestion_source_rejected sourceAccount={} reason={}",
			maskAccount(sourceAccount),
			normalize(reason)
		);
		record(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.OPERATIONAL, "operational_ingestion_source_rejected", PersistedAuditEventStatus.REJECTED)
			.primaryReference(maskAccount(sourceAccount))
			.detailCode(normalize(reason))
			.build());
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
		record(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.OPERATIONAL, "operational_ingestion_accepted", PersistedAuditEventStatus.SUCCESS)
			.householdId(householdId)
			.primaryReference(maskAccount(sourceAccount))
			.secondaryReference(ingestionId == null ? "-" : ingestionId.toString())
			.detailCode(decision == null ? "-" : decision.name())
			.safeContext("duplicate", duplicate)
			.build());
	}

	public void requestRateLimited(String path, String keyId, String sourceAccount, RateLimitExceededException exception) {
		LOGGER.warn(
			"event=operational_request_rate_limited path={} keyId={} sourceAccount={} retryAfterSeconds={} maxRequests={} windowSeconds={}",
			normalize(path),
			normalize(keyId),
			maskAccount(sourceAccount),
			exception.getRetryAfterSeconds(),
			exception.getMaxRequests(),
			exception.getWindowSeconds()
		);
		record(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.OPERATIONAL, "operational_request_rate_limited", PersistedAuditEventStatus.LIMITED)
			.sourceKey(normalize(keyId))
			.requestPath(normalize(path))
			.primaryReference(maskAccount(sourceAccount))
			.detailCode(exception.getScope().name())
			.safeContext("retryAfterSeconds", exception.getRetryAfterSeconds())
			.safeContext("maxRequests", exception.getMaxRequests())
			.safeContext("windowSeconds", exception.getWindowSeconds())
			.build());
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

	private void record(PersistedAuditEventCommand command) {
		if (auditEventRecorder != null) {
			auditEventRecorder.recordSafely(command);
		}
	}
}
