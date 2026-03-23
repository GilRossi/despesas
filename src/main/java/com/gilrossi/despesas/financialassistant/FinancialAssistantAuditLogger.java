package com.gilrossi.despesas.financialassistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FinancialAssistantAuditLogger {

	private static final Logger log = LoggerFactory.getLogger(FinancialAssistantAuditLogger.class);

	public void queryStarted(FinancialAssistantAccessContext context, String referenceMonth) {
		log.info(
			"event=assistant_query_started userId={} householdId={} role={} referenceMonth={}",
			context.userId(),
			context.householdId(),
			context.role(),
			sanitizeReferenceMonth(referenceMonth)
		);
	}

	public void queryDenied(FinancialAssistantContextException exception) {
		log.warn(
			"event=assistant_query_denied reason={} userId={} householdId={} role={}",
			exception.reasonCode(),
			exception.userId(),
			exception.householdId(),
			exception.role()
		);
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
	}

	public void queryFailed(FinancialAssistantAccessContext context, RuntimeException exception) {
		log.warn(
			"event=assistant_query_failed userId={} householdId={} role={} exceptionClass={}",
			context == null ? null : context.userId(),
			context == null ? null : context.householdId(),
			context == null ? null : context.role(),
			exception.getClass().getSimpleName()
		);
	}

	private String sanitizeReferenceMonth(String referenceMonth) {
		return referenceMonth == null ? "" : referenceMonth.trim();
	}
}
