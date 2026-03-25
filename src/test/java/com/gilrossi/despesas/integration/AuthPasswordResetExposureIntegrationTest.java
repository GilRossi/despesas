package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

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
import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.identity.HouseholdMemberRepository;
import com.gilrossi.despesas.security.RefreshTokenRecord;
import com.gilrossi.despesas.security.RefreshTokenRecordRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	"app.security.expose-reset-token=true",
	"app.security.password-reset-secret=test-password-reset-secret"
})
class AuthPasswordResetExposureIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private RefreshTokenRecordRepository refreshTokenRecordRepository;

	@Autowired
	private HouseholdMemberRepository householdMemberRepository;

	@BeforeEach
	void clean() {
		refreshTokenRecordRepository.deleteAll();
		householdMemberRepository.deleteAll();
		appUserRepository.deleteAll();
	}

	@Test
	void deve_redefinir_senha_e_revogar_tokens_quando_token_for_exposto_em_ambiente_de_teste() throws Exception {
		RegistrationResponse response = registrationService.register(new RegistrationRequest("Ana", "ana@example.com", "Senha123!", "House"));
		AppUser user = appUserRepository.findById(response.userId()).orElseThrow();
		RefreshTokenRecord record = new RefreshTokenRecord("tid", "fid", user.getId(), "hash", Instant.now().plusSeconds(3600));
		refreshTokenRecordRepository.save(record);

		String body = mockMvc.perform(post("/api/v1/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "email": "ana@example.com" }
					"""))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.resetToken").isNotEmpty())
			.andReturn().getResponse().getContentAsString();

		JsonNode node = objectMapper.readTree(body);
		String token = node.get("resetToken").asText();

		mockMvc.perform(post("/api/v1/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "token": "%s", "newPassword": "NovaSenha123!", "newPasswordConfirmation": "NovaSenha123!" }
					""".formatted(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.success").value(true));

		assertThat(refreshTokenRecordRepository.findByUserIdAndRevokedAtIsNull(user.getId())).isEmpty();
		AppUser updated = appUserRepository.findById(user.getId()).orElseThrow();
		assertThat(updated.getPasswordHash()).isNotEqualTo(user.getPasswordHash());
	}
}
