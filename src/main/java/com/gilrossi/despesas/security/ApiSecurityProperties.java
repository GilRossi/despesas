package com.gilrossi.despesas.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record ApiSecurityProperties(
	@NotBlank(message = "app.security.token-secret must be configured")
	String tokenSecret,
	@NotBlank(message = "app.security.password-reset-secret must be configured")
	String passwordResetSecret,
	List<String> corsAllowedOriginPatterns,
	boolean exposeResetToken
) {
}
