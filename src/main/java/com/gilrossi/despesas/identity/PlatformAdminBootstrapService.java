package com.gilrossi.despesas.identity;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class PlatformAdminBootstrapService implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlatformAdminBootstrapService.class);

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final PlatformAdminBootstrapProperties properties;

	public PlatformAdminBootstrapService(
		AppUserRepository appUserRepository,
		PasswordEncoder passwordEncoder,
		PlatformAdminBootstrapProperties properties
	) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		bootstrapIfNeeded();
	}

	@Transactional
	public void bootstrapIfNeeded() {
		if (!properties.enabled()) {
			LOGGER.info("Platform admin bootstrap disabled by configuration");
			return;
		}
		if (appUserRepository.existsByPlatformRoleAndDeletedAtIsNull(PlatformUserRole.PLATFORM_ADMIN)) {
			return;
		}

		String name = sanitize(properties.name());
		String email = sanitizeEmail(properties.email());
		String password = properties.password();
		if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
			LOGGER.warn("Platform admin bootstrap skipped because credentials are missing");
			return;
		}
		if (!StringUtils.hasText(name)) {
			name = "Platform Admin";
		}

		appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email).ifPresent(existing -> {
			throw new IllegalStateException("Bootstrap admin email already belongs to a non-admin user");
		});

		AppUser admin = new AppUser(
			name,
			email,
			passwordEncoder.encode(password),
			PlatformUserRole.PLATFORM_ADMIN
		);
		appUserRepository.save(admin);
		LOGGER.info("Platform admin bootstrap completed for {}", maskEmail(email));
	}

	private String sanitize(String value) {
		return value == null ? null : value.trim();
	}

	private String sanitizeEmail(String value) {
		String sanitized = sanitize(value);
		return sanitized == null ? null : sanitized.toLowerCase(Locale.ROOT);
	}

	private String maskEmail(String email) {
		int atIndex = email.indexOf('@');
		if (atIndex <= 1) {
			return "***";
		}
		return email.charAt(0) + "***" + email.substring(atIndex);
	}
}
