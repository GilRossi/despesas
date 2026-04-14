package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.PlatformUserRole;

@SpringBootTest
@AutoConfigureMockMvc
class AdminPlatformFoundationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin-platform-foundation@local.invalid")
			.ifPresent(appUserRepository::delete);
		appUserRepository.save(new AppUser(
			"Platform Admin",
			"admin-platform-foundation@local.invalid",
			passwordEncoder.encode("senha123"),
			PlatformUserRole.PLATFORM_ADMIN
		));
	}

	@Test
	void deve_criar_listar_detalhar_e_atualizar_modulos_do_espaco() throws Exception {
		String adminToken = login("admin-platform-foundation@local.invalid", "senha123");

		String createResponse = mockMvc.perform(post("/api/v1/admin/spaces")
				.header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "spaceName":"Espaco Admin Driver",
					  "ownerName":"Owner Admin Driver",
					  "ownerEmail":"owner-admin-driver@local.invalid",
					  "ownerPassword":"senha123",
					  "enabledModules":["FINANCIAL","DRIVER"]
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.spaceName").value("Espaco Admin Driver"))
			.andExpect(jsonPath("$.data.owner.email").value("owner-admin-driver@local.invalid"))
			.andExpect(jsonPath("$.data.modules[0].key").value("FINANCIAL"))
			.andExpect(jsonPath("$.data.modules[0].enabled").value(true))
			.andExpect(jsonPath("$.data.modules[0].mandatory").value(true))
			.andExpect(jsonPath("$.data.modules[1].key").value("DRIVER"))
			.andExpect(jsonPath("$.data.modules[1].enabled").value(true))
			.andReturn()
			.getResponse()
			.getContentAsString();

		long spaceId = objectMapper.readTree(createResponse).path("data").path("spaceId").asLong();

		mockMvc.perform(get("/api/v1/admin/spaces")
				.header("Authorization", bearer(adminToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].spaceId").value(spaceId))
			.andExpect(jsonPath("$.data[0].spaceName").value("Espaco Admin Driver"))
			.andExpect(jsonPath("$.data[0].modules[0].key").value("FINANCIAL"))
			.andExpect(jsonPath("$.data[0].modules[1].key").value("DRIVER"));

		mockMvc.perform(get("/api/v1/admin/spaces/{spaceId}", spaceId)
				.header("Authorization", bearer(adminToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.spaceId").value(spaceId))
			.andExpect(jsonPath("$.data.activeMembersCount").value(1))
			.andExpect(jsonPath("$.data.owner.name").value("Owner Admin Driver"));

		mockMvc.perform(put("/api/v1/admin/spaces/{spaceId}/modules", spaceId)
				.header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "enabledModules":[]
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.modules[0].key").value("FINANCIAL"))
			.andExpect(jsonPath("$.data.modules[0].enabled").value(true))
			.andExpect(jsonPath("$.data.modules[1].key").value("DRIVER"))
			.andExpect(jsonPath("$.data.modules[1].enabled").value(false));
	}

	@Test
	void deve_expor_visao_geral_e_saude_minimas_do_admin_platform() throws Exception {
		String adminToken = login("admin-platform-foundation@local.invalid", "senha123");

		mockMvc.perform(get("/api/v1/admin/platform/overview")
				.header("Authorization", bearer(adminToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalSpaces").isNumber())
			.andExpect(jsonPath("$.data.activeSpaces").isNumber())
			.andExpect(jsonPath("$.data.totalUsers").isNumber())
			.andExpect(jsonPath("$.data.totalPlatformAdmins").isNumber())
			.andExpect(jsonPath("$.data.modules[0].key").value("FINANCIAL"))
			.andExpect(jsonPath("$.data.modules[0].mandatory").value(true))
			.andExpect(jsonPath("$.data.modules[1].key").value("DRIVER"))
			.andExpect(jsonPath("$.data.actuator.healthExposed").value(true))
			.andExpect(jsonPath("$.data.actuator.infoExposed").value(true))
			.andExpect(jsonPath("$.data.actuator.metricsExposed").value(false));

		mockMvc.perform(get("/api/v1/admin/platform/health")
				.header("Authorization", bearer(adminToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.applicationStatus").value("UP"))
			.andExpect(jsonPath("$.data.actuator.healthExposed").value(true))
			.andExpect(jsonPath("$.data.actuator.metricsExposed").value(false))
			.andExpect(jsonPath("$.data.jvm.availableProcessors").isNumber())
			.andExpect(jsonPath("$.data.jvm.heapUsedBytes").isNumber())
			.andExpect(jsonPath("$.data.jvm.uptimeMs").isNumber());
	}

	@Test
	void deve_restringir_endpoints_admin_platform_para_platform_admin() throws Exception {
		String adminToken = login("admin-platform-foundation@local.invalid", "senha123");

		String createResponse = mockMvc.perform(post("/api/v1/admin/spaces")
				.header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "spaceName":"Espaco Restrito",
					  "ownerName":"Owner Restrito",
					  "ownerEmail":"owner-restrito@local.invalid",
					  "ownerPassword":"senha123"
					}
					"""))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		String ownerToken = login("owner-restrito@local.invalid", "senha123");
		long spaceId = objectMapper.readTree(createResponse).path("data").path("spaceId").asLong();

		mockMvc.perform(get("/api/v1/admin/spaces")
				.header("Authorization", bearer(ownerToken)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		mockMvc.perform(put("/api/v1/admin/spaces/{spaceId}/modules", spaceId)
				.header("Authorization", bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "enabledModules":["FINANCIAL","DRIVER"]
					}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	private String login(String email, String password) throws Exception {
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

		JsonNode jsonNode = objectMapper.readTree(response);
		assertThat(jsonNode.path("data").path("accessToken").asText()).isNotBlank();
		return jsonNode.path("data").path("accessToken").asText();
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}
}
