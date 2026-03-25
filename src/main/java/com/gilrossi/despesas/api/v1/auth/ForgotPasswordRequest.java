package com.gilrossi.despesas.api.v1.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
	@NotBlank @Email String email
) {
}
