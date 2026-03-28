package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SpaceReferenceIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void deve_criar_referencia_persistir_normalizado_e_sugerir_existente_duplicada() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"space-reference-owner@local.invalid",
			"senha123",
			"Casa Reference"
		));

		String token = loginApi("space-reference-owner@local.invalid", "senha123");

		String createResponse = mockMvc.perform(post("/api/v1/space/references")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "type":"ESCRITORIO",
					  "name":"  Escritório   Central  "
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.result").value("CREATED"))
			.andExpect(jsonPath("$.data.reference.type").value("ESCRITORIO"))
			.andExpect(jsonPath("$.data.reference.typeGroup").value("COMERCIAL_TRABALHO"))
			.andExpect(jsonPath("$.data.reference.name").value("Escritório Central"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		Long createdId = objectMapper.readTree(createResponse).path("data").path("reference").path("id").asLong();
		String normalizedName = jdbcTemplate.queryForObject(
			"select normalized_name from space_references where id = ?",
			String.class,
			createdId
		);

		assertThat(normalizedName).isEqualTo("escritorio central");

		mockMvc.perform(post("/api/v1/space/references")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "type":"ESCRITORIO",
					  "name":"escritorio central"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.result").value("DUPLICATE_SUGGESTION"))
			.andExpect(jsonPath("$.data.reference").doesNotExist())
			.andExpect(jsonPath("$.data.suggestedReference.id").value(createdId))
			.andExpect(jsonPath("$.data.message").value("Encontrei uma referência parecida no seu Espaço. Quer usar essa para evitar duplicidade?"));
	}

	@Test
	void deve_isolar_households_e_filtrar_listagem() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"space-reference-ana@local.invalid",
			"senha123",
			"Casa Ana"
		));
		registrationService.register(new RegistrationRequest(
			"Bruno",
			"space-reference-bruno@local.invalid",
			"senha123",
			"Casa Bruno"
		));

		String tokenAna = loginApi("space-reference-ana@local.invalid", "senha123");
		String tokenBruno = loginApi("space-reference-bruno@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/space/references")
				.header("Authorization", bearer(tokenAna))
				.contentType("application/json")
				.content("""
					{
					  "type":"CLIENTE",
					  "name":"Projeto Acme"
					}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/space/references")
				.header("Authorization", bearer(tokenAna))
				.contentType("application/json")
				.content("""
					{
					  "type":"CARRO",
					  "name":"Carro de apoio"
					}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/space/references")
				.header("Authorization", bearer(tokenBruno))
				.contentType("application/json")
				.content("""
					{
					  "type":"CLIENTE",
					  "name":"Projeto Acme"
					}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/space/references")
				.header("Authorization", bearer(tokenAna))
				.param("typeGroup", "COMERCIAL_TRABALHO")
				.param("q", "projeto"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].type").value("CLIENTE"))
			.andExpect(jsonPath("$.data[0].name").value("Projeto Acme"));

		String responseAna = mockMvc.perform(get("/api/v1/space/references")
				.header("Authorization", bearer(tokenAna)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		String responseBruno = mockMvc.perform(get("/api/v1/space/references")
				.header("Authorization", bearer(tokenBruno)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		JsonNode dataAna = objectMapper.readTree(responseAna).path("data");
		JsonNode dataBruno = objectMapper.readTree(responseBruno).path("data");

		assertThat(dataAna).hasSize(2);
		assertThat(dataBruno).hasSize(1);
		assertThat(dataAna.findValuesAsText("name")).contains("Projeto Acme", "Carro de apoio");
		assertThat(dataBruno.findValuesAsText("name")).containsExactly("Projeto Acme");
	}

	private String loginApi(String email, String password) throws Exception {
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
		return objectMapper.readTree(response).path("data").path("accessToken").asText();
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}
}
