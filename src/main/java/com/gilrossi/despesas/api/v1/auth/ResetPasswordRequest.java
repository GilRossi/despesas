package com.gilrossi.despesas.api.v1.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
	@NotBlank String token,
	@NotBlank @Size(min = 6, max = 120) String newPassword,
	@NotBlank @Size(min = 6, max = 120) String newPasswordConfirmation
) {
}
