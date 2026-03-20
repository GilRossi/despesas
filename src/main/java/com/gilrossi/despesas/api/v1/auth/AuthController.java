package com.gilrossi.despesas.api.v1.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.identity.AuthResponse;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.security.CurrentUserProvider;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final RegistrationService registrationService;
	private final CurrentUserProvider currentUserProvider;
	private final MobileAuthService mobileAuthService;

	public AuthController(
		RegistrationService registrationService,
		CurrentUserProvider currentUserProvider,
		MobileAuthService mobileAuthService
	) {
		this.registrationService = registrationService;
		this.currentUserProvider = currentUserProvider;
		this.mobileAuthService = mobileAuthService;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<RegistrationResponse>> register(@Valid @RequestBody RegistrationRequest request) {
		return ResponseEntity.status(201).body(new ApiResponse<>(registrationService.register(request)));
	}

	@PostMapping("/login")
	public ApiResponse<MobileAuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return new ApiResponse<>(mobileAuthService.login(request));
	}

	@PostMapping("/refresh")
	public ApiResponse<MobileAuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return new ApiResponse<>(mobileAuthService.refresh(request));
	}

	@GetMapping("/me")
	public ApiResponse<AuthResponse> me() {
		var principal = currentUserProvider.requireCurrentUser();
		return new ApiResponse<>(new AuthResponse(
			principal.getUserId(),
			principal.getHouseholdId(),
			principal.getUsername(),
			principal.getDisplayName(),
			principal.getRole()
		));
	}
}
