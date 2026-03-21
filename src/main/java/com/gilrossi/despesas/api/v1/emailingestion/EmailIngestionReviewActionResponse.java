package com.gilrossi.despesas.api.v1.emailingestion;

import com.gilrossi.despesas.emailingestion.EmailIngestionReviewActionResult;

public record EmailIngestionReviewActionResponse(
	Long ingestionId,
	com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision decision,
	com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason decisionReason,
	Long expenseId
) {

	public static EmailIngestionReviewActionResponse from(EmailIngestionReviewActionResult result) {
		return new EmailIngestionReviewActionResponse(
			result.ingestionId(),
			result.finalDecision(),
			result.decisionReason(),
			result.importedExpenseId()
		);
	}
}
