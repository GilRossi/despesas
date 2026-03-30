package com.gilrossi.despesas.fixedbill;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;

public record FixedBillResponse(
	Long id,
	String description,
	BigDecimal amount,
	LocalDate firstDueDate,
	FixedBillFrequency frequency,
	ReferenceResponse category,
	ReferenceResponse subcategory,
	ReferenceResponse spaceReference,
	boolean active,
	Instant createdAt
) {
}
