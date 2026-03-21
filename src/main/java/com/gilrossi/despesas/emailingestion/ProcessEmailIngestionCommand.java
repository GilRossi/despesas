package com.gilrossi.despesas.emailingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ProcessEmailIngestionCommand(
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
	List<EmailIngestionItem> items,
	String summary,
	EmailIngestionClassification classification,
	BigDecimal confidence,
	String rawReference,
	EmailIngestionDesiredDecision desiredDecision
) {
}
