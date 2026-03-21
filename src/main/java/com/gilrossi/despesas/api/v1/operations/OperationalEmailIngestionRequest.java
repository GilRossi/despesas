package com.gilrossi.despesas.api.v1.operations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.gilrossi.despesas.emailingestion.EmailIngestionClassification;
import com.gilrossi.despesas.emailingestion.EmailIngestionDesiredDecision;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OperationalEmailIngestionRequest(
	@NotBlank(message = "sourceAccount must not be blank")
	@Size(max = 160, message = "sourceAccount must have at most 160 characters")
	String sourceAccount,
	@NotBlank(message = "externalMessageId must not be blank")
	@Size(max = 255, message = "externalMessageId must have at most 255 characters")
	String externalMessageId,
	@NotBlank(message = "sender must not be blank")
	@Size(max = 255, message = "sender must have at most 255 characters")
	String sender,
	@NotBlank(message = "subject must not be blank")
	@Size(max = 255, message = "subject must have at most 255 characters")
	String subject,
	@NotNull(message = "receivedAt must not be null")
	OffsetDateTime receivedAt,
	@Size(max = 140, message = "merchantOrPayee must have at most 140 characters")
	String merchantOrPayee,
	@Size(max = 80, message = "suggestedCategoryName must have at most 80 characters")
	String suggestedCategoryName,
	@Size(max = 80, message = "suggestedSubcategoryName must have at most 80 characters")
	String suggestedSubcategoryName,
	@Positive(message = "totalAmount must be greater than zero")
	BigDecimal totalAmount,
	LocalDate dueDate,
	LocalDate occurredOn,
	@NotBlank(message = "currency must not be blank")
	@Size(min = 3, max = 3, message = "currency must have exactly 3 characters")
	String currency,
	@Valid
	@Size(max = 50, message = "items must contain at most 50 entries")
	List<OperationalEmailIngestionItemRequest> items,
	@Size(max = 500, message = "summary must have at most 500 characters")
	String summary,
	@NotNull(message = "classification must not be null")
	EmailIngestionClassification classification,
	@NotNull(message = "confidence must not be null")
	@DecimalMin(value = "0.0", message = "confidence must be between 0 and 1")
	@DecimalMax(value = "1.0", message = "confidence must be between 0 and 1")
	BigDecimal confidence,
	@NotBlank(message = "rawReference must not be blank")
	@Size(max = 500, message = "rawReference must have at most 500 characters")
	String rawReference,
	@NotNull(message = "desiredDecision must not be null")
	EmailIngestionDesiredDecision desiredDecision
) {
}
