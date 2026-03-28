package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OnboardingIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Test
	void deve_expor_estado_inicial_do_onboarding_em_login_me_e_refresh() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"onboarding-auth@local.invalid",
			"senha123",
			"Casa Auth"
		));

		JsonNode login = login("onboarding-auth@local.invalid", "senha123");
		String accessToken = login.path("data").path("accessToken").asText();
		String refreshToken = login.path("data").path("refreshToken").asText();

		assertThat(login.path("data").path("user").path("onboarding").path("completed").asBoolean()).isFalse();
		assertThat(login.path("data").path("user").path("onboarding").path("completedAt").isNull()).isTrue();

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", bearer(accessToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.onboarding.completed").value(false))
			.andExpect(jsonPath("$.data.onboarding.completedAt").isEmpty());

		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType("application/json")
				.content("""
					{
					  "refreshToken":"%s"
					}
					""".formatted(refreshToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.user.onboarding.completed").value(false))
			.andExpect(jsonPath("$.data.user.onboarding.completedAt").isEmpty());
	}

	@Test
	void deve_concluir_onboarding_de_forma_idempotente_e_refletir_no_auth() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"onboarding-complete@local.invalid",
			"senha123",
			"Casa Complete"
		));

		JsonNode login = login("onboarding-complete@local.invalid", "senha123");
		String accessToken = login.path("data").path("accessToken").asText();
		String refreshToken = login.path("data").path("refreshToken").asText();
		Long userId = login.path("data").path("user").path("userId").asLong();

		String firstCompletionResponse = mockMvc.perform(post("/api/v1/onboarding/complete")
				.header("Authorization", bearer(accessToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.completed").value(true))
			.andExpect(jsonPath("$.data.completedAt").isString())
			.andReturn()
			.getResponse()
			.getContentAsString();

		String completedAt = objectMapper.readTree(firstCompletionResponse).path("data").path("completedAt").asText();

		mockMvc.perform(post("/api/v1/onboarding/complete")
				.header("Authorization", bearer(accessToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.completed").value(true))
			.andExpect(jsonPath("$.data.completedAt").value(completedAt));

		AppUser user = appUserRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow();
		assertThat(user.isOnboardingCompleted()).isTrue();
		assertThat(user.getOnboardingCompletedAt()).isNotNull();
		assertThat(user.getOnboardingCompletedAt().toString()).isEqualTo(completedAt);

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", bearer(accessToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.onboarding.completed").value(true))
			.andExpect(jsonPath("$.data.onboarding.completedAt").value(completedAt));

		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType("application/json")
				.content("""
					{
					  "refreshToken":"%s"
					}
					""".formatted(refreshToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.user.onboarding.completed").value(true))
			.andExpect(jsonPath("$.data.user.onboarding.completedAt").value(completedAt));
	}

	private JsonNode login(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
				.contentType("application/json")
				.content("""
					{
					  "email":"%s",
					  "password":"%s"
					}
					""".formatted(email, password)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(response);
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}
}
