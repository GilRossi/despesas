package com.gilrossi.despesas.emailingestion;

public record EmailIngestionReviewActionResult(
	Long ingestionId,
	EmailIngestionFinalDecision finalDecision,
	EmailIngestionDecisionReason decisionReason,
	Long importedExpenseId
) {
}
