package com.gilrossi.despesas.emailingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record EmailIngestionRecord(
	Long id,
	Long householdId,
	Long sourceId,
	String sourceAccount,
	String normalizedSourceAccount,
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
	EmailIngestionClassification classification,
	BigDecimal confidence,
	String rawReference,
	EmailIngestionDesiredDecision desiredDecision,
	EmailIngestionFinalDecision finalDecision,
	EmailIngestionDecisionReason decisionReason,
	String fingerprint,
	Long importedExpenseId,
	OffsetDateTime createdAt,
	OffsetDateTime updatedAt,
	List<EmailIngestionItem> items
) {
}
