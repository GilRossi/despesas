package com.gilrossi.despesas.api.v1.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordResetRequest(
	@NotBlank(message = "targetEmail must not be blank")
	@Email(message = "targetEmail must be valid")
	@Size(max = 160, message = "targetEmail must have at most 160 characters")
	String targetEmail,

	@NotBlank(message = "newPassword must not be blank")
	@Size(min = 6, max = 120, message = "newPassword must have between 6 and 120 characters")
	String newPassword,

	@NotBlank(message = "newPasswordConfirmation must not be blank")
	@Size(min = 6, max = 120, message = "newPasswordConfirmation must have between 6 and 120 characters")
	String newPasswordConfirmation
) {
}
