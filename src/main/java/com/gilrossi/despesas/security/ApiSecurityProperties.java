package com.gilrossi.despesas.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record ApiSecurityProperties(
	@NotBlank(message = "app.security.token-secret must be configured")
	String tokenSecret
) {
}
