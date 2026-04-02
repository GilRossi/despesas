package com.gilrossi.despesas.fixedbill;

import java.time.Instant;
import java.time.LocalDate;

public record FixedBillGeneratedExpenseResponse(
	Long expenseId,
	LocalDate dueDate,
	Instant createdAt
) {
}
