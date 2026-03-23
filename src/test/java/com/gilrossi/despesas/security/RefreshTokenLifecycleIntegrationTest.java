package com.gilrossi.despesas.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenLifecycleIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RefreshTokenRecordRepository refreshTokenRecordRepository;

	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void setUp() {
		Logger logger = (Logger) LoggerFactory.getLogger(SecurityAuditLogger.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
	}

	@AfterEach
	void tearDown() {
		Logger logger = (Logger) LoggerFactory.getLogger(SecurityAuditLogger.class);
		logger.detachAppender(logAppender);
	}

	@Test
	void deve_persistir_rotacionar_e_revogar_refresh_token_sem_vazar_token_nos_logs() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"refresh-lifecycle@local.invalid",
			"senha123",
			"Casa Refresh"
		));

		JsonNode login = login("refresh-lifecycle@local.invalid", "senha123");
		String accessToken = login.path("data").path("accessToken").asText();
		String initialRefreshToken = login.path("data").path("refreshToken").asText();
		String initialTokenId = tokenId(initialRefreshToken);

		RefreshTokenRecord initialRecord = findRecord(initialTokenId);
		assertThat(initialRecord.getRevokedAt()).isNull();
		assertThat(initialRecord.getRevocationReason()).isNull();

		JsonNode refresh = responseTree(refresh(initialRefreshToken)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.refreshToken").isString())
		);

		String rotatedRefreshToken = refresh.path("data").path("refreshToken").asText();
		String rotatedTokenId = tokenId(rotatedRefreshToken);

		RefreshTokenRecord rotatedOldRecord = findRecord(initialTokenId);
		RefreshTokenRecord activeReplacementRecord = findRecord(rotatedTokenId);
		assertThat(rotatedOldRecord.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.ROTATED);
		assertThat(rotatedOldRecord.getReplacedByTokenId()).isEqualTo(rotatedTokenId);
		assertThat(rotatedOldRecord.getLastUsedAt()).isNotNull();
		assertThat(activeReplacementRecord.getFamilyId()).isEqualTo(rotatedOldRecord.getFamilyId());
		assertThat(activeReplacementRecord.getRevokedAt()).isNull();
		assertThat(refreshTokenRecordRepository.findByFamilyIdAndRevokedAtIsNull(rotatedOldRecord.getFamilyId()))
			.extracting(RefreshTokenRecord::getTokenId)
			.containsExactly(rotatedTokenId);

		mockMvc.perform(post("/api/v1/auth/logout")
				.header("Authorization", bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "refreshToken":"%s"
					}
					""".formatted(rotatedRefreshToken)))
			.andExpect(status().isNoContent());

		RefreshTokenRecord revokedReplacementRecord = findRecord(rotatedTokenId);
		assertThat(revokedReplacementRecord.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.LOGOUT);
		assertThat(refreshTokenRecordRepository.findByFamilyIdAndRevokedAtIsNull(rotatedOldRecord.getFamilyId())).isEmpty();

		refresh(rotatedRefreshToken)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		assertAuditEventsWithoutRawTokens(
			List.of("event=auth_login_success", "event=auth_refresh_success", "event=auth_logout_success", "event=auth_refresh_rejected"),
			List.of(initialRefreshToken, rotatedRefreshToken)
		);
	}

	@Test
	void deve_revogar_toda_a_familia_quando_refresh_rotacionado_for_reutilizado() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Bia",
			"refresh-reuse@local.invalid",
			"senha123",
			"Casa Reuse"
		));

		JsonNode login = login("refresh-reuse@local.invalid", "senha123");
		String initialRefreshToken = login.path("data").path("refreshToken").asText();
		String initialTokenId = tokenId(initialRefreshToken);

		JsonNode refresh = responseTree(refresh(initialRefreshToken)
			.andExpect(status().isOk())
		);
		String rotatedRefreshToken = refresh.path("data").path("refreshToken").asText();
		String rotatedTokenId = tokenId(rotatedRefreshToken);

		refresh(initialRefreshToken)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		refresh(rotatedRefreshToken)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		RefreshTokenRecord oldRecord = findRecord(initialTokenId);
		RefreshTokenRecord replacementRecord = findRecord(rotatedTokenId);
		assertThat(oldRecord.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.ROTATED);
		assertThat(replacementRecord.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.REUSE_DETECTED);
		assertThat(refreshTokenRecordRepository.findByFamilyIdAndRevokedAtIsNull(oldRecord.getFamilyId())).isEmpty();

		assertAuditEventsWithoutRawTokens(
			List.of("event=auth_refresh_success", "event=auth_refresh_rejected", "reason=reuse_detected"),
			List.of(initialRefreshToken, rotatedRefreshToken)
		);
	}

	private JsonNode login(String email, String password) throws Exception {
		String response = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
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

	private RefreshTokenRecord findRecord(String tokenId) {
		return refreshTokenRecordRepository.findAll().stream()
			.filter(record -> tokenId.equals(record.getTokenId()))
			.findFirst()
			.orElseThrow();
	}

	private String tokenId(String rawRefreshToken) {
		return rawRefreshToken.split("\\.", 2)[0];
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}

	private void assertAuditEventsWithoutRawTokens(List<String> expectedFragments, List<String> forbiddenFragments) {
		List<String> messages = logAppender.list.stream()
			.map(ILoggingEvent::getFormattedMessage)
			.toList();
		assertThat(messages).isNotEmpty();
		for (String expectedFragment : expectedFragments) {
			assertThat(messages).anyMatch(message -> message.contains(expectedFragment));
		}
		for (String forbiddenFragment : forbiddenFragments) {
			assertThat(messages).noneMatch(message -> message.contains(forbiddenFragment));
		}
	}
}
