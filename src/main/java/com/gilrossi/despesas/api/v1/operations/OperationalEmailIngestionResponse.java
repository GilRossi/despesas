package com.gilrossi.despesas.api.v1.operations;

import com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason;
import com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision;
import com.gilrossi.despesas.emailingestion.EmailIngestionResult;

public record OperationalEmailIngestionResponse(
	Long ingestionId,
	Long householdId,
	EmailIngestionFinalDecision decision,
	EmailIngestionDecisionReason reason,
	Long expenseId,
	boolean duplicate,
	String message
) {
	public static OperationalEmailIngestionResponse from(EmailIngestionResult result) {
		return new OperationalEmailIngestionResponse(
			result.ingestionId(),
			result.householdId(),
			result.decision(),
			result.reason(),
			result.expenseId(),
			result.duplicate(),
			result.message()
		);
	}
}
