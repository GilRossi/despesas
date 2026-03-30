package com.gilrossi.despesas.expense;

import java.time.LocalDate;

import com.gilrossi.despesas.payment.PaymentMethod;

import jakarta.validation.constraints.NotNull;

public record CreateExpenseInitialPaymentRequest(
	@NotNull(message = "initialPayment.paidAt must not be null")
	LocalDate paidAt,
	@NotNull(message = "initialPayment.method must not be null")
	PaymentMethod method
) {
}
