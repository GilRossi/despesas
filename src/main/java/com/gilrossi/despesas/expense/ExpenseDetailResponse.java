package com.gilrossi.despesas.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;
import com.gilrossi.despesas.payment.PaymentResponse;

public record ExpenseDetailResponse(
	Long id,
	String description,
	BigDecimal amount,
	LocalDate dueDate,
	ExpenseContext context,
	ReferenceResponse category,
	ReferenceResponse subcategory,
	String notes,
	ExpenseStatus status,
	BigDecimal paidAmount,
	BigDecimal remainingAmount,
	int paymentsCount,
	boolean overdue,
	Instant createdAt,
	Instant updatedAt,
	List<PaymentResponse> payments
) {
}
