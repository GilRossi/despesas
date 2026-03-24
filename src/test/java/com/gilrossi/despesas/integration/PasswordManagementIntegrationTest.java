package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.audit.PersistedAuditEvent;
import com.gilrossi.despesas.audit.PersistedAuditEventRepository;
import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.PlatformUserRole;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.ratelimit.RateLimitCounterRepository;
import com.gilrossi.despesas.security.RefreshTokenRecord;
import com.gilrossi.despesas.security.RefreshTokenRecordRepository;
import com.gilrossi.despesas.security.RefreshTokenRevocationReason;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordManagementIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RefreshTokenRecordRepository refreshTokenRecordRepository;

	@Autowired
	private PersistedAuditEventRepository persistedAuditEventRepository;

	@Autowired
	private RateLimitCounterRepository rateLimitCounterRepository;

	private ListAppender<ILoggingEvent> logAppender;

	@BeforeEach
	void setUp() {
		persistedAuditEventRepository.deleteAll();
		rateLimitCounterRepository.deleteAll();
		Logger logger = (Logger) LoggerFactory.getLogger(com.gilrossi.despesas.security.SecurityAuditLogger.class);
		logAppender = new ListAppender<>();
		logAppender.start();
		logger.addAppender(logAppender);
	}

	@AfterEach
	void tearDown() {
		Logger logger = (Logger) LoggerFactory.getLogger(com.gilrossi.despesas.security.SecurityAuditLogger.class);
		logger.detachAppender(logAppender);
	}

	@Test
	void deve_trocar_a_propria_senha_e_invalidar_tokens_anteriores() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"password-change-owner@local.invalid",
			"senha123",
			"Casa Change"
		));

		JsonNode login = login("password-change-owner@local.invalid", "senha123");
		String accessToken = login.path("data").path("accessToken").asText();
		String refreshToken = login.path("data").path("refreshToken").asText();
		String tokenId = tokenId(refreshToken);

		mockMvc.perform(post("/api/v1/auth/change-password")
				.header("Authorization", bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "currentPassword":"senha123",
					  "newPassword":"novaSenha456",
					  "newPasswordConfirmation":"novaSenha456"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.revokedRefreshTokens").value(1))
			.andExpect(jsonPath("$.data.reauthenticationRequired").value(true));

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", bearer(accessToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "refreshToken":"%s"
					}
					""".formatted(refreshToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email":"password-change-owner@local.invalid",
					  "password":"senha123"
					}
					"""))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email":"password-change-owner@local.invalid",
					  "password":"novaSenha456"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.user.email").value("password-change-owner@local.invalid"));

		RefreshTokenRecord revokedToken = findRecord(tokenId);
		assertThat(revokedToken.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.PASSWORD_CHANGED);

		assertAuditEventsWithoutSecrets(
			List.of("auth_password_change_success"),
			List.of("senha123", "novaSenha456", refreshToken)
		);
		assertLogMessagesWithoutSecrets(
			List.of("event=auth_password_change_success"),
			List.of("senha123", "novaSenha456", refreshToken)
		);
	}

	@Test
	void deve_permitir_platform_admin_resetar_senha_de_usuario_padrao_e_invalidar_tokens_antigos() throws Exception {
		appUserRepository.save(new AppUser(
			"Platform Admin",
			"password-reset-admin@local.invalid",
			passwordEncoder.encode("senha123"),
			PlatformUserRole.PLATFORM_ADMIN
		));
		RegistrationResponse owner = registrationService.register(new RegistrationRequest(
			"Owner Resetavel",
			"password-reset-owner@local.invalid",
			"senha123",
			"Casa Reset"
		));

		JsonNode adminLogin = login("password-reset-admin@local.invalid", "senha123");
		String adminAccessToken = adminLogin.path("data").path("accessToken").asText();

		JsonNode ownerLogin = login("password-reset-owner@local.invalid", "senha123");
		String ownerAccessToken = ownerLogin.path("data").path("accessToken").asText();
		String ownerRefreshToken = ownerLogin.path("data").path("refreshToken").asText();
		String ownerRefreshTokenId = tokenId(ownerRefreshToken);

		mockMvc.perform(post("/api/v1/admin/users/password-reset")
				.header("Authorization", bearer(adminAccessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "targetEmail":"password-reset-owner@local.invalid",
					  "newPassword":"novaSenha789",
					  "newPasswordConfirmation":"novaSenha789"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.targetEmailMasked").value("p***@local.invalid"))
			.andExpect(jsonPath("$.data.revokedRefreshTokens").value(1));

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", bearer(ownerAccessToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "refreshToken":"%s"
					}
					""".formatted(ownerRefreshToken)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email":"%s",
					  "password":"senha123"
					}
					""".formatted(owner.email())))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email":"%s",
					  "password":"novaSenha789"
					}
					""".formatted(owner.email())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.user.email").value(owner.email()));

		RefreshTokenRecord revokedToken = findRecord(ownerRefreshTokenId);
		assertThat(revokedToken.getRevocationReason()).isEqualTo(RefreshTokenRevocationReason.PASSWORD_RESET);

		assertAuditEventsWithoutSecrets(
			List.of("auth_password_reset_success"),
			List.of("senha123", "novaSenha789", ownerRefreshToken)
		);
		assertLogMessagesWithoutSecrets(
			List.of("event=auth_password_reset_success"),
			List.of("senha123", "novaSenha789", ownerRefreshToken)
		);
	}

	@Test
	void deve_bloquear_reset_para_nao_admin_e_para_alvo_platform_admin() throws Exception {
		appUserRepository.save(new AppUser(
			"Platform Admin",
			"blocked-reset-admin@local.invalid",
			passwordEncoder.encode("senha123"),
			PlatformUserRole.PLATFORM_ADMIN
		));
		appUserRepository.save(new AppUser(
			"Outro Platform Admin",
			"blocked-reset-target-admin@local.invalid",
			passwordEncoder.encode("senha456"),
			PlatformUserRole.PLATFORM_ADMIN
		));
		registrationService.register(new RegistrationRequest(
			"Owner Sem Permissao",
			"blocked-reset-owner@local.invalid",
			"senha123",
			"Casa Sem Permissao"
		));

		String ownerAccessToken = login("blocked-reset-owner@local.invalid", "senha123").path("data").path("accessToken").asText();
		String adminAccessToken = login("blocked-reset-admin@local.invalid", "senha123").path("data").path("accessToken").asText();

		mockMvc.perform(post("/api/v1/admin/users/password-reset")
				.header("Authorization", bearer(ownerAccessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "targetEmail":"blocked-reset-owner@local.invalid",
					  "newPassword":"novaSenha789",
					  "newPasswordConfirmation":"novaSenha789"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		mockMvc.perform(post("/api/v1/admin/users/password-reset")
				.header("Authorization", bearer(adminAccessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "targetEmail":"blocked-reset-target-admin@local.invalid",
					  "newPassword":"novaSenha789",
					  "newPasswordConfirmation":"novaSenha789"
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.message").value("Platform admins must use self-service password change"));

		assertThat(persistedAuditEventRepository.findAll())
			.extracting(PersistedAuditEvent::getEventType)
			.contains("access_denied", "auth_password_reset_rejected");
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

	private RefreshTokenRecord findRecord(String tokenId) {
		return refreshTokenRecordRepository.findAll().stream()
			.filter(record -> tokenId.equals(record.getTokenId()))
			.findFirst()
			.orElseThrow();
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}

	private String tokenId(String rawRefreshToken) {
		return rawRefreshToken.split("\\.", 2)[0];
	}

	private void assertAuditEventsWithoutSecrets(List<String> expectedEventTypes, List<String> forbiddenFragments) {
		List<PersistedAuditEvent> events = persistedAuditEventRepository.findAll();
		assertThat(events).extracting(PersistedAuditEvent::getEventType).containsAll(expectedEventTypes);
		for (String forbiddenFragment : forbiddenFragments) {
			assertThat(events).noneMatch(event -> contains(event.getPrimaryReference(), forbiddenFragment));
			assertThat(events).noneMatch(event -> contains(event.getSecondaryReference(), forbiddenFragment));
			assertThat(events).noneMatch(event -> contains(event.getSafeContextJson(), forbiddenFragment));
		}
	}

	private void assertLogMessagesWithoutSecrets(List<String> expectedFragments, List<String> forbiddenFragments) {
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

	private boolean contains(String value, String fragment) {
		return value != null && fragment != null && value.contains(fragment);
	}
}
