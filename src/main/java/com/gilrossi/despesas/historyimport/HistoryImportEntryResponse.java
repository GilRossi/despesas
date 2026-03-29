package com.gilrossi.despesas.historyimport;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gilrossi.despesas.expense.ExpenseStatus;

public record HistoryImportEntryResponse(
	Long expenseId,
	Long paymentId,
	String description,
	BigDecimal amount,
	LocalDate date,
	ExpenseStatus status
) {
}
