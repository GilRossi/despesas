package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
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
import com.gilrossi.despesas.ratelimit.RateLimitCounterRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PlatformAdminAccessIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private PersistedAuditEventRepository persistedAuditEventRepository;

	@Autowired
	private RateLimitCounterRepository rateLimitCounterRepository;

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		persistedAuditEventRepository.deleteAll();
		rateLimitCounterRepository.deleteAll();
	}

	@Test
	void deve_permitir_fluxo_admin_owner_member_sem_signup_publico() throws Exception {
		String adminEmail = "platform-admin-flow@local.invalid";
		appUserRepository.save(new AppUser(
			"Platform Admin",
			adminEmail,
			passwordEncoder.encode("senha123"),
			PlatformUserRole.PLATFORM_ADMIN
		));

		JsonNode adminLogin = login(adminEmail, "senha123");
		String adminToken = adminLogin.path("data").path("accessToken").asText();
		assertThat(adminLogin.path("data").path("user").path("role").asText()).isEqualTo("PLATFORM_ADMIN");
		assertThat(adminLogin.path("data").path("user").path("householdId").isNull()).isTrue();

		mockMvc.perform(get("/api/v1/household/members")
				.header("Authorization", bearer(adminToken)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		mockMvc.perform(post("/api/v1/household/members")
				.header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name":"Member Indevido",
					  "email":"member-admin-blocked@local.invalid",
					  "password":"senha123"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		String ownerEmail = "owner-controlled@local.invalid";
		mockMvc.perform(post("/api/v1/admin/households")
				.header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "householdName":"Casa Controlada",
					  "ownerName":"Owner Controlado",
					  "ownerEmail":"%s",
					  "ownerPassword":"senha123"
					}
					""".formatted(ownerEmail)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.ownerEmail").value(ownerEmail))
			.andExpect(jsonPath("$.data.ownerRole").value("OWNER"));

		JsonNode ownerLogin = login(ownerEmail, "senha123");
		String ownerToken = ownerLogin.path("data").path("accessToken").asText();
		long householdId = ownerLogin.path("data").path("user").path("householdId").asLong();
		assertThat(ownerLogin.path("data").path("user").path("role").asText()).isEqualTo("OWNER");

		String memberEmail = "member-controlled@local.invalid";
		mockMvc.perform(post("/api/v1/household/members")
				.header("Authorization", bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name":"Member Controlado",
					  "email":"%s",
					  "password":"senha123"
					}
					""".formatted(memberEmail)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.role").value("MEMBER"));

		JsonNode memberLogin = login(memberEmail, "senha123");
		String memberToken = memberLogin.path("data").path("accessToken").asText();
		assertThat(memberLogin.path("data").path("user").path("role").asText()).isEqualTo("MEMBER");
		assertThat(memberLogin.path("data").path("user").path("householdId").asLong()).isEqualTo(householdId);

		mockMvc.perform(post("/api/v1/admin/households")
				.header("Authorization", bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "householdName":"Outra Casa",
					  "ownerName":"Outro Owner",
					  "ownerEmail":"owner-bloqueado@local.invalid",
					  "ownerPassword":"senha123"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		mockMvc.perform(post("/api/v1/household/members")
				.header("Authorization", bearer(memberToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name":"Member Bloqueado",
					  "email":"member-bloqueado@local.invalid",
					  "password":"senha123"
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		assertThat(persistedAuditEventRepository.findAll())
			.anyMatch(event -> "access_denied".equals(event.getEventType())
				&& "/api/v1/household/members".equals(event.getRequestPath()));
		assertThat(persistedAuditEventRepository.findAll())
			.extracting(PersistedAuditEvent::getEventType)
			.contains("access_denied");
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

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}
}
