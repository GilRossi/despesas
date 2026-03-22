package com.gilrossi.despesas.api.v1.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateHouseholdOwnerRequest(
	@NotBlank(message = "householdName must not be blank")
	@Size(max = 120, message = "householdName must have at most 120 characters")
	String householdName,

	@NotBlank(message = "ownerName must not be blank")
	@Size(max = 120, message = "ownerName must have at most 120 characters")
	String ownerName,

	@NotBlank(message = "ownerEmail must not be blank")
	@Email(message = "ownerEmail must be valid")
	@Size(max = 160, message = "ownerEmail must have at most 160 characters")
	String ownerEmail,

	@NotBlank(message = "ownerPassword must not be blank")
	@Size(min = 6, max = 120, message = "ownerPassword must have between 6 and 120 characters")
	String ownerPassword
) {
}
