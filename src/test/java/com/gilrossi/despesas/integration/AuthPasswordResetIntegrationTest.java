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
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.PlatformUserRole;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.HouseholdMemberRepository;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.security.RefreshTokenRecord;
import com.gilrossi.despesas.security.RefreshTokenRecordRepository;
import com.gilrossi.despesas.security.RefreshTokenRevocationReason;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPasswordResetIntegrationTest {

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
	void deve_responder_neutro_no_forgot_password_sem_expor_token() throws Exception {
		registrationService.register(new RegistrationRequest("Gil", "gil@example.com", "Senha123!", "House"));

		mockMvc.perform(post("/api/v1/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "email": "gil@example.com" }
					"""))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.maskedEmail").value("g***@example.com"))
			.andExpect(jsonPath("$.resetToken").doesNotExist());

		mockMvc.perform(post("/api/v1/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "email": "naoexiste@example.com" }
					"""))
			.andExpect(status().isAccepted())
			.andExpect(jsonPath("$.maskedEmail").value("n***@example.com"))
			.andExpect(jsonPath("$.resetToken").doesNotExist());
	}

	@Test
	void deve_rejeitar_token_invalido() throws Exception {
		mockMvc.perform(post("/api/v1/auth/reset-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{ "token": "token-invalido", "newPassword": "NovaSenha123!", "newPasswordConfirmation": "NovaSenha123!" }
					"""))
			.andExpect(status().isUnauthorized());
	}
}
