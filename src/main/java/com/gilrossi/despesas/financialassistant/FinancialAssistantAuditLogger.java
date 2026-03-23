package com.gilrossi.despesas.financialassistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.gilrossi.despesas.audit.PersistedAuditEventCategory;
import com.gilrossi.despesas.audit.PersistedAuditEventCommand;
import com.gilrossi.despesas.audit.PersistedAuditEventRecorder;
import com.gilrossi.despesas.audit.PersistedAuditEventStatus;
import com.gilrossi.despesas.ratelimit.RateLimitExceededException;

@Component
public class FinancialAssistantAuditLogger {

	private static final Logger log = LoggerFactory.getLogger(FinancialAssistantAuditLogger.class);
	private final PersistedAuditEventRecorder auditEventRecorder;

	@Autowired
	public FinancialAssistantAuditLogger(ObjectProvider<PersistedAuditEventRecorder> auditEventRecorderProvider) {
		this(auditEventRecorderProvider.getIfAvailable());
	}

	FinancialAssistantAuditLogger() {
		this((PersistedAuditEventRecorder) null);
	}

	FinancialAssistantAuditLogger(PersistedAuditEventRecorder auditEventRecorder) {
		this.auditEventRecorder = auditEventRecorder;
	}

	public void queryStarted(FinancialAssistantAccessContext context, String referenceMonth) {
		log.info(
			"event=assistant_query_started userId={} householdId={} role={} referenceMonth={}",
			context.userId(),
			context.householdId(),
			context.role(),
			sanitizeReferenceMonth(referenceMonth)
		);
		record(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.ASSISTANT, "assistant_query_started", PersistedAuditEventStatus.STARTED)
			.userId(context.userId())
			.householdId(context.householdId())
			.actorRole(context.role())
			.primaryReference(sanitizeReferenceMonth(referenceMonth))
			.build());
	}

	public void queryDenied(FinancialAssistantContextException exception) {
		log.warn(
			"event=assistant_query_denied reason={} userId={} householdId={} role={}",
			exception.reasonCode(),
			exception.userId(),
			exception.householdId(),
			exception.role()
		);
		record(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.ASSISTANT, "assistant_query_denied", PersistedAuditEventStatus.DENIED)
			.userId(exception.userId())
			.householdId(exception.householdId())
			.actorRole(exception.role())
			.detailCode(exception.reasonCode())
			.build());
	}

	public void queryCompleted(
		FinancialAssistantAccessContext context,
		FinancialAssistantIntent intent,
		FinancialAssistantQueryMode mode
	) {
		log.info(
			"event=assistant_query_completed userId={} householdId={} role={} intent={} mode={}",
			context.userId(),
			context.householdId(),
			context.role(),
			intent,
			mode
		);
		record(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.ASSISTANT, "assistant_query_completed", PersistedAuditEventStatus.SUCCESS)
			.userId(context.userId())
			.householdId(context.householdId())
			.actorRole(context.role())
			.primaryReference(intent == null ? "-" : intent.name())
			.secondaryReference(mode == null ? "-" : mode.name())
			.safeContext("responseMode", mode == null ? "-" : mode.name())
			.build());
	}

	public void queryFailed(FinancialAssistantAccessContext context, RuntimeException exception) {
		log.warn(
			"event=assistant_query_failed userId={} householdId={} role={} exceptionClass={}",
			context == null ? null : context.userId(),
			context == null ? null : context.householdId(),
			context == null ? null : context.role(),
			exception.getClass().getSimpleName()
		);
		record(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.ASSISTANT, "assistant_query_failed", PersistedAuditEventStatus.FAILURE)
			.userId(context == null ? null : context.userId())
			.householdId(context == null ? null : context.householdId())
			.actorRole(context == null ? null : context.role())
			.detailCode(exception.getClass().getSimpleName())
			.build());
	}

	public void queryRateLimited(
		FinancialAssistantAccessContext context,
		String referenceMonth,
		RateLimitExceededException exception
	) {
		log.warn(
			"event=assistant_query_rate_limited userId={} householdId={} role={} referenceMonth={} retryAfterSeconds={} maxRequests={} windowSeconds={}",
			context.userId(),
			context.householdId(),
			context.role(),
			sanitizeReferenceMonth(referenceMonth),
			exception.getRetryAfterSeconds(),
			exception.getMaxRequests(),
			exception.getWindowSeconds()
		);
		record(PersistedAuditEventCommand
			.event(PersistedAuditEventCategory.ASSISTANT, "assistant_query_rate_limited", PersistedAuditEventStatus.LIMITED)
			.userId(context.userId())
			.householdId(context.householdId())
			.actorRole(context.role())
			.primaryReference(sanitizeReferenceMonth(referenceMonth))
			.detailCode(exception.getScope().name())
			.safeContext("retryAfterSeconds", exception.getRetryAfterSeconds())
			.safeContext("maxRequests", exception.getMaxRequests())
			.safeContext("windowSeconds", exception.getWindowSeconds())
			.build());
	}

	private String sanitizeReferenceMonth(String referenceMonth) {
		return referenceMonth == null ? "" : referenceMonth.trim();
	}

	private void record(PersistedAuditEventCommand command) {
		if (auditEventRecorder != null) {
			auditEventRecorder.recordSafely(command);
		}
	}
}
