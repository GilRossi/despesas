package com.gilrossi.despesas.api.v1.auth;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
	@NotBlank(message = "refreshToken must not be blank")
	String refreshToken
) {
}
