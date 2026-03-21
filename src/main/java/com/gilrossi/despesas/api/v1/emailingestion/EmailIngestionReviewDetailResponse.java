package com.gilrossi.despesas.api.v1.emailingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.gilrossi.despesas.emailingestion.EmailIngestionRecord;

public record EmailIngestionReviewDetailResponse(
	Long ingestionId,
	String sourceAccount,
	String externalMessageId,
	String sender,
	String subject,
	OffsetDateTime receivedAt,
	String merchantOrPayee,
	String suggestedCategoryName,
	String suggestedSubcategoryName,
	BigDecimal totalAmount,
	LocalDate dueDate,
	LocalDate occurredOn,
	String currency,
	String summary,
	com.gilrossi.despesas.emailingestion.EmailIngestionClassification classification,
	BigDecimal confidence,
	String rawReference,
	com.gilrossi.despesas.emailingestion.EmailIngestionDesiredDecision desiredDecision,
	com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision finalDecision,
	com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason decisionReason,
	Long importedExpenseId,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt,
	List<EmailIngestionReviewItemResponse> items
) {

	public static EmailIngestionReviewDetailResponse from(EmailIngestionRecord record) {
		return new EmailIngestionReviewDetailResponse(
			record.id(),
			record.sourceAccount(),
			record.externalMessageId(),
			record.sender(),
			record.subject(),
			record.receivedAt(),
			record.merchantOrPayee(),
			record.suggestedCategoryName(),
			record.suggestedSubcategoryName(),
			record.totalAmount(),
			record.dueDate(),
			record.occurredOn(),
			record.currency(),
			record.summary(),
			record.classification(),
			record.confidence(),
			record.rawReference(),
			record.desiredDecision(),
			record.finalDecision(),
			record.decisionReason(),
			record.importedExpenseId(),
			record.createdAt(),
			record.updatedAt(),
			record.items() == null ? List.of() : record.items().stream().map(EmailIngestionReviewItemResponse::from).toList()
		);
	}
}
