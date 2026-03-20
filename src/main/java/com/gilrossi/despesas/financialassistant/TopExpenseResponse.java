package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gilrossi.despesas.expense.ExpenseContext;

public record TopExpenseResponse(
	Long expenseId,
	String description,
	BigDecimal amount,
	LocalDate dueDate,
	String categoryName,
	String subcategoryName,
	ExpenseContext context
) {
}
