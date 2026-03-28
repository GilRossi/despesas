package com.gilrossi.despesas.api.v1.space;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.gilrossi.despesas.spacereference.SpaceReferenceType;

public record CreateSpaceReferenceRequest(
	@NotNull(message = "type must not be null")
	SpaceReferenceType type,

	@NotBlank(message = "name must not be blank")
	@Size(max = 120, message = "name must have at most 120 characters")
	String name
) {
}
