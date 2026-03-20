package com.gilrossi.despesas.api.v1.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
	@NotBlank(message = "email must not be blank")
	@Email(message = "email must be valid")
	@Size(max = 160, message = "email must have at most 160 characters")
	String email,

	@NotBlank(message = "password must not be blank")
	@Size(min = 6, max = 120, message = "password must have between 6 and 120 characters")
	String password
) {
}
