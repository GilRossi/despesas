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
class DriverModuleGateIntegrationTest {

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
		appUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("driver-gate-admin@local.invalid")
			.ifPresent(appUserRepository::delete);
		appUserRepository.save(new AppUser(
			"Driver Gate Admin",
			"driver-gate-admin@local.invalid",
			passwordEncoder.encode("senha123"),
			PlatformUserRole.PLATFORM_ADMIN
		));
	}

	@Test
	void deve_bloquear_quando_modulo_driver_estiver_desabilitado_no_espaco_atual() throws Exception {
		String adminToken = login("driver-gate-admin@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/admin/spaces")
				.header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "spaceName":"Espaco Driver Desligado",
					  "ownerName":"Owner Driver Desligado",
					  "ownerEmail":"owner-driver-off@local.invalid",
					  "ownerPassword":"senha123"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.modules[0].key").value("FINANCIAL"))
			.andExpect(jsonPath("$.data.modules[0].enabled").value(true))
			.andExpect(jsonPath("$.data.modules[1].key").value("DRIVER"))
			.andExpect(jsonPath("$.data.modules[1].enabled").value(false));

		String ownerToken = login("owner-driver-off@local.invalid", "senha123");

		mockMvc.perform(get("/api/v1/driver/_probe")
				.header("Authorization", bearer(ownerToken)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void deve_permitir_quando_modulo_driver_estiver_habilitado_e_bloquear_apos_desabilitar() throws Exception {
		String adminToken = login("driver-gate-admin@local.invalid", "senha123");

		String createResponse = mockMvc.perform(post("/api/v1/admin/spaces")
				.header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "spaceName":"Espaco Driver Ligado",
					  "ownerName":"Owner Driver Ligado",
					  "ownerEmail":"owner-driver-on@local.invalid",
					  "ownerPassword":"senha123",
					  "enabledModules":["FINANCIAL","DRIVER"]
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.modules[1].key").value("DRIVER"))
			.andExpect(jsonPath("$.data.modules[1].enabled").value(true))
			.andReturn()
			.getResponse()
			.getContentAsString();

		long spaceId = objectMapper.readTree(createResponse).path("data").path("spaceId").asLong();
		String ownerToken = login("owner-driver-on@local.invalid", "senha123");

		mockMvc.perform(get("/api/v1/driver/_probe")
				.header("Authorization", bearer(ownerToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.moduleKey").value("DRIVER"))
			.andExpect(jsonPath("$.data.enabled").value(true))
			.andExpect(jsonPath("$.data.spaceId").value(spaceId));

		mockMvc.perform(put("/api/v1/admin/spaces/{spaceId}/modules", spaceId)
				.header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "enabledModules":[]
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.modules[1].key").value("DRIVER"))
			.andExpect(jsonPath("$.data.modules[1].enabled").value(false));

		mockMvc.perform(get("/api/v1/driver/_probe")
				.header("Authorization", bearer(ownerToken)))
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
