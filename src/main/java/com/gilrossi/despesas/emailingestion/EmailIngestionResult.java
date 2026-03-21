package com.gilrossi.despesas.emailingestion;

public record EmailIngestionResult(
	Long ingestionId,
	Long householdId,
	EmailIngestionFinalDecision decision,
	EmailIngestionDecisionReason reason,
	Long expenseId,
	boolean duplicate,
	String message
) {
}
