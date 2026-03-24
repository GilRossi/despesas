package com.gilrossi.despesas.api.v1.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
	@NotBlank(message = "currentPassword must not be blank")
	@Size(min = 6, max = 120, message = "currentPassword must have between 6 and 120 characters")
	String currentPassword,

	@NotBlank(message = "newPassword must not be blank")
	@Size(min = 6, max = 120, message = "newPassword must have between 6 and 120 characters")
	String newPassword,

	@NotBlank(message = "newPasswordConfirmation must not be blank")
	@Size(min = 6, max = 120, message = "newPasswordConfirmation must have between 6 and 120 characters")
	String newPasswordConfirmation
) {
}
