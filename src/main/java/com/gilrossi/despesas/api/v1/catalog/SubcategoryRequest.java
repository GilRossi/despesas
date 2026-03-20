package com.gilrossi.despesas.api.v1.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubcategoryRequest(
	@NotNull(message = "categoryId must not be null")
	Long categoryId,
	@NotBlank(message = "name must not be blank")
	@Size(max = 80, message = "name must have at most 80 characters")
	String name,
	Boolean active
) {
}
