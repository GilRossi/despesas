package com.gilrossi.despesas.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.audit.PersistedAuditEvent;
import com.gilrossi.despesas.audit.PersistedAuditEventRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.ratelimit.RateLimitCounterRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	"app.rate-limits.auth-login.max-requests=1",
	"app.rate-limits.auth-login.window-seconds=300",
	"app.rate-limits.auth-refresh.max-requests=1",
	"app.rate-limits.auth-refresh.window-seconds=300"
})
class AuthRateLimitIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PersistedAuditEventRepository persistedAuditEventRepository;

	@Autowired
	private RateLimitCounterRepository rateLimitCounterRepository;

	@BeforeEach
	void setUp() {
		persistedAuditEventRepository.deleteAll();
		rateLimitCounterRepository.deleteAll();
	}

	@Test
	void deve_limitar_tentativas_de_login_e_persistir_evento_de_abuso() throws Exception {
		registrationService.register(new RegistrationRequest("Ana", "auth-rate-login@local.invalid", "senha123", "Casa Login"));

		login("auth-rate-login@local.invalid", "senha123")
			.andExpect(status().isOk());

		login("auth-rate-login@local.invalid", "senha123")
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(jsonPath("$.code").value("RATE_LIMITED"));

		assertThat(persistedAuditEventRepository.findAllByEventTypeOrderByIdAsc("auth_login_rate_limited"))
			.singleElement()
			.satisfies(event -> {
				assertThat(event.getStatus()).isEqualTo(com.gilrossi.despesas.audit.PersistedAuditEventStatus.LIMITED);
				assertThat(event.getPrimaryReference()).contains("@local.invalid").doesNotContain("auth-rate-login@local.invalid");
				assertThat(event.getSafeContextJson()).doesNotContain("senha123");
			});
	}

	@Test
	void deve_limitar_rotacao_de_refresh_e_persistir_evento_sem_vazar_token() throws Exception {
		registrationService.register(new RegistrationRequest("Bia", "auth-rate-refresh@local.invalid", "senha123", "Casa Refresh"));

		JsonNode login = responseTree(login("auth-rate-refresh@local.invalid", "senha123")
			.andExpect(status().isOk()));
		String initialRefreshToken = login.path("data").path("refreshToken").asText();

		JsonNode refresh = responseTree(refresh(initialRefreshToken)
			.andExpect(status().isOk()));
		String rotatedRefreshToken = refresh.path("data").path("refreshToken").asText();

		refresh(rotatedRefreshToken)
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(jsonPath("$.code").value("RATE_LIMITED"));

		assertThat(persistedAuditEventRepository.findAllByEventTypeOrderByIdAsc("auth_refresh_rate_limited"))
			.singleElement()
			.satisfies(event -> {
				assertThat(event.getStatus()).isEqualTo(com.gilrossi.despesas.audit.PersistedAuditEventStatus.LIMITED);
				assertThat(event.getSafeContextJson()).doesNotContain(initialRefreshToken).doesNotContain(rotatedRefreshToken);
				assertThat(event.getPrimaryReference()).isNull();
				assertThat(event.getSecondaryReference()).doesNotContain(initialRefreshToken).doesNotContain(rotatedRefreshToken);
			});
		assertThat(persistedAuditEventRepository.findAll())
			.extracting(PersistedAuditEvent::getEventType)
			.contains("auth_refresh_success", "auth_refresh_rate_limited");
	}

	private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "email":"%s",
				  "password":"%s"
				}
				""".formatted(email, password)));
	}

	private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/refresh")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "refreshToken":"%s"
				}
				""".formatted(refreshToken)));
	}

	private JsonNode responseTree(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
		return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
	}
}
