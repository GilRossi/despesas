package com.gilrossi.despesas.fixedbill;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateFixedBillRequest(
	@NotBlank(message = "description must not be blank")
	@Size(max = 140, message = "description must have at most 140 characters")
	String description,
	@NotNull(message = "amount must not be null")
	@Positive(message = "amount must be greater than zero")
	BigDecimal amount,
	@NotNull(message = "firstDueDate must not be null")
	LocalDate firstDueDate,
	@NotNull(message = "frequency must not be null")
	FixedBillFrequency frequency,
	@NotNull(message = "categoryId must not be null")
	Long categoryId,
	@NotNull(message = "subcategoryId must not be null")
	Long subcategoryId,
	Long spaceReferenceId
) {
}
