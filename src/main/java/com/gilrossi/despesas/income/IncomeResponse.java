package com.gilrossi.despesas.income;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;

public record IncomeResponse(
	Long id,
	String description,
	BigDecimal amount,
	LocalDate receivedOn,
	ReferenceResponse spaceReference,
	Instant createdAt
) {
}
