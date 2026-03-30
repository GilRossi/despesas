package com.gilrossi.despesas.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;

public record ExpenseResponse(
	Long id,
	String description,
	BigDecimal amount,
	LocalDate dueDate,
	LocalDate occurredOn,
	ExpenseContext context,
	ReferenceResponse category,
	ReferenceResponse subcategory,
	ReferenceResponse reference,
	String notes,
	ExpenseStatus status,
	BigDecimal paidAmount,
	BigDecimal remainingAmount,
	int paymentsCount,
	boolean overdue,
	Instant createdAt,
	Instant updatedAt
) {
}
