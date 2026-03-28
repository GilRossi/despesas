package com.gilrossi.despesas.identity;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {

	private final AppUserRepository appUserRepository;
	private final Clock clock;

	@Autowired
	public OnboardingService(AppUserRepository appUserRepository) {
		this(appUserRepository, Clock.systemUTC());
	}

	OnboardingService(AppUserRepository appUserRepository, Clock clock) {
		this.appUserRepository = appUserRepository;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public OnboardingStatusResponse currentStatus(Long userId) {
		return OnboardingStatusResponse.from(loadUser(userId));
	}

	@Transactional
	public OnboardingStatusResponse complete(Long userId) {
		AppUser user = loadUser(userId);
		if (!user.isOnboardingCompleted() || user.getOnboardingCompletedAt() == null) {
			user.markOnboardingCompleted(clock.instant().truncatedTo(ChronoUnit.MICROS));
		}
		return OnboardingStatusResponse.from(user);
	}

	private AppUser loadUser(Long userId) {
		return appUserRepository.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new AppUserNotFoundException("User not found"));
	}
}
