package com.gilrossi.despesas.api.v1.onboarding;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.identity.OnboardingService;
import com.gilrossi.despesas.identity.OnboardingStatusResponse;
import com.gilrossi.despesas.security.CurrentUserProvider;

@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

	private final CurrentUserProvider currentUserProvider;
	private final OnboardingService onboardingService;

	public OnboardingController(
		CurrentUserProvider currentUserProvider,
		OnboardingService onboardingService
	) {
		this.currentUserProvider = currentUserProvider;
		this.onboardingService = onboardingService;
	}

	@PostMapping("/complete")
	public ApiResponse<OnboardingStatusResponse> complete() {
		var principal = currentUserProvider.requireCurrentUser();
		return new ApiResponse<>(onboardingService.complete(principal.getUserId()));
	}
}
