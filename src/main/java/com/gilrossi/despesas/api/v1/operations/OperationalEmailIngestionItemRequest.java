package com.gilrossi.despesas.api.v1.operations;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record OperationalEmailIngestionItemRequest(
	@Size(max = 255, message = "items.description must have at most 255 characters")
	String description,
	@Positive(message = "items.amount must be greater than zero")
	BigDecimal amount,
	@Positive(message = "items.quantity must be greater than zero")
	BigDecimal quantity
) {
}
