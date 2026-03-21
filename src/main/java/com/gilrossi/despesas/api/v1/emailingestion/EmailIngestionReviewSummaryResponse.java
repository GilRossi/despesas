package com.gilrossi.despesas.api.v1.emailingestion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.gilrossi.despesas.emailingestion.EmailIngestionRecord;

public record EmailIngestionReviewSummaryResponse(
	Long ingestionId,
	String sourceAccount,
	String sender,
	String subject,
	OffsetDateTime receivedAt,
	String merchantOrPayee,
	BigDecimal totalAmount,
	String currency,
	String summary,
	com.gilrossi.despesas.emailingestion.EmailIngestionClassification classification,
	BigDecimal confidence,
	com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason decisionReason
) {

	public static EmailIngestionReviewSummaryResponse from(EmailIngestionRecord record) {
		return new EmailIngestionReviewSummaryResponse(
			record.id(),
			record.sourceAccount(),
			record.sender(),
			record.subject(),
			record.receivedAt(),
			record.merchantOrPayee(),
			record.totalAmount(),
			record.currency(),
			record.summary(),
			record.classification(),
			record.confidence(),
			record.decisionReason()
		);
	}
}
