package com.gilrossi.despesas.payment;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePaymentRequest(
	@NotNull(message = "expenseId must not be null")
	Long expenseId,
	@NotNull(message = "amount must not be null")
	@Positive(message = "amount must be greater than zero")
	BigDecimal amount,
	@NotNull(message = "paidAt must not be null")
	LocalDate paidAt,
	@NotNull(message = "method must not be null")
	PaymentMethod method,
	@Size(max = 255, message = "notes must have at most 255 characters")
	String notes
) {
}
