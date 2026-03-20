package com.gilrossi.despesas.expense;

import java.time.LocalDate;

public record ExpenseFilter(
	String q,
	ExpenseContext context,
	Long categoryId,
	Long subcategoryId,
	ExpenseStatus status,
	Boolean overdue,
	LocalDate dueDateFrom,
	LocalDate dueDateTo,
	Boolean hasPayments
) {
}
