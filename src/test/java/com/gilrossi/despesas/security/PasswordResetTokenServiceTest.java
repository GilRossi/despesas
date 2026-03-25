package com.gilrossi.despesas.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.security.ApiIssuedToken;

class PasswordResetTokenServiceTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void deve_emitir_token_com_ttl_de_15_minutos() {
		Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
		PasswordResetTokenService service = new PasswordResetTokenService(objectMapper, fixedClock, "secret-test");

		ApiIssuedToken issued = service.issue(1L, "user@example.com");

		assertThat(issued.expiresAt())
			.isEqualTo(Instant.parse("2024-01-01T00:15:00Z"));
	}

	@Test
	void deve_rejeitar_token_expirado() {
		Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:20:00Z"), ZoneOffset.UTC);
		PasswordResetTokenService service = new PasswordResetTokenService(objectMapper, fixedClock, "secret-test");
		ApiIssuedToken issued = service.issue(1L, "user@example.com");

		Clock later = Clock.fixed(Instant.parse("2024-01-01T00:36:00Z"), ZoneOffset.UTC);
		PasswordResetTokenService lateService = new PasswordResetTokenService(objectMapper, later, "secret-test");

		assertThatThrownBy(() -> lateService.parse(issued.value()))
			.isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
	}
}
