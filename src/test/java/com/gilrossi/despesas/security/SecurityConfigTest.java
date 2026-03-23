package com.gilrossi.despesas.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class SecurityConfigTest {

	@Test
	void deve_criar_api_token_service_quando_token_secret_estiver_configurado() {
		SecurityConfig securityConfig = new SecurityConfig();
		assertNotNull(securityConfig.apiTokenService(
			new ObjectMapper(),
			new ApiSecurityProperties("test-token-secret", List.of("http://localhost:*"))
		));
	}
}
