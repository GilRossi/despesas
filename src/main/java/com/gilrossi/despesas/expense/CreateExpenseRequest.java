package com.gilrossi.despesas.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record CreateExpenseRequest(
	@NotBlank(message = "description must not be blank")
	@Size(max = 140, message = "description must have at most 140 characters")
	String description,
	@NotNull(message = "amount must not be null")
	@Positive(message = "amount must be greater than zero")
	BigDecimal amount,
	@NotNull(message = "occurredOn must not be null")
	LocalDate occurredOn,
	LocalDate dueDate,
	@NotNull(message = "categoryId must not be null")
	Long categoryId,
	@NotNull(message = "subcategoryId must not be null")
	Long subcategoryId,
	Long spaceReferenceId,
	@Size(max = 255, message = "notes must have at most 255 characters")
	String notes,
	@Valid
	CreateExpenseInitialPaymentRequest initialPayment
) {
	public CreateExpenseRequest(
		String description,
		BigDecimal amount,
		LocalDate occurredOn,
		LocalDate dueDate,
		Long categoryId,
		Long subcategoryId,
		Long spaceReferenceId,
		String notes
	) {
		this(
			description,
			amount,
			occurredOn,
			dueDate,
			categoryId,
			subcategoryId,
			spaceReferenceId,
			notes,
			null
		);
	}
}
