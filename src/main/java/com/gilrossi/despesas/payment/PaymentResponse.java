package com.gilrossi.despesas.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.gilrossi.despesas.expense.ExpenseStatus;

public record PaymentResponse(
	Long id,
	Long expenseId,
	BigDecimal amount,
	LocalDate paidAt,
	PaymentMethod method,
	String notes,
	ExpenseStatus expenseStatus,
	BigDecimal expensePaidAmount,
	BigDecimal expenseRemainingAmount,
	Instant createdAt,
	Instant updatedAt
) {
}
