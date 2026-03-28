package com.gilrossi.despesas.identity;

import java.time.Instant;

public record OnboardingStatusResponse(
	boolean completed,
	Instant completedAt
) {

	public static OnboardingStatusResponse from(AppUser user) {
		return new OnboardingStatusResponse(
			user.isOnboardingCompleted(),
			user.getOnboardingCompletedAt()
		);
	}
}
