package com.gilrossi.despesas.api.v1.admin;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.security.CurrentUserProvider;
import com.gilrossi.despesas.security.PasswordManagementService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
public class PlatformAdminUserPasswordController {

	private final CurrentUserProvider currentUserProvider;
	private final PasswordManagementService passwordManagementService;

	public PlatformAdminUserPasswordController(
		CurrentUserProvider currentUserProvider,
		PasswordManagementService passwordManagementService
	) {
		this.currentUserProvider = currentUserProvider;
		this.passwordManagementService = passwordManagementService;
	}

	@PostMapping("/password-reset")
	public ApiResponse<AdminPasswordResetResponse> resetPassword(@Valid @RequestBody AdminPasswordResetRequest request) {
		return new ApiResponse<>(passwordManagementService.resetPassword(
			currentUserProvider.requireCurrentUser(),
			request
		));
	}
}
