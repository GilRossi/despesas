package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.income.Income;
import com.gilrossi.despesas.income.IncomeRepository;

@SpringBootTest
@AutoConfigureMockMvc
class IncomeIntegrationTest {

	@Autowired
	private org.springframework.test.web.servlet.MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private IncomeRepository incomeRepository;

	@Test
	void deve_criar_ganho_com_referencia_opcional_valida_e_persistir_no_household_correto() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"income-owner@local.invalid",
			"senha123",
			"Casa da Ana"
		));

		String token = loginApi("income-owner@local.invalid", "senha123");

		String referenceResponse = mockMvc.perform(post("/api/v1/space/references")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "type":"CLIENTE",
					  "name":"Projeto Horizonte"
					}
					"""))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		long spaceReferenceId = objectMapper.readTree(referenceResponse)
			.path("data")
			.path("reference")
			.path("id")
			.asLong();

		String createIncomeResponse = mockMvc.perform(post("/api/v1/incomes")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "description":"Freelance de março",
					  "amount":1800.00,
					  "receivedOn":"2026-03-28",
					  "spaceReferenceId":%s
					}
					""".formatted(spaceReferenceId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.description").value("Freelance de março"))
			.andExpect(jsonPath("$.data.amount").value(1800.00))
			.andExpect(jsonPath("$.data.receivedOn").value("2026-03-28"))
			.andExpect(jsonPath("$.data.spaceReference.id").value(spaceReferenceId))
			.andExpect(jsonPath("$.data.spaceReference.name").value("Projeto Horizonte"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		long incomeId = objectMapper.readTree(createIncomeResponse).path("data").path("id").asLong();
		Income persisted = incomeRepository.findById(incomeId).orElseThrow();

		assertThat(persisted.getDescription()).isEqualTo("Freelance de março");
		assertThat(persisted.getAmount()).isEqualByComparingTo(new BigDecimal("1800.00"));
		assertThat(persisted.getReceivedOn()).isEqualTo(LocalDate.of(2026, 3, 28));
		assertThat(persisted.getSpaceReferenceId()).isEqualTo(spaceReferenceId);
		assertThat(persisted.getHouseholdId()).isNotNull();
		assertThat(persisted.getCreatedAt()).isNotNull();
	}

	@Test
	void deve_rejeitar_referencia_de_outro_household_ao_criar_ganho() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"income-ana@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		registrationService.register(new RegistrationRequest(
			"Bruno",
			"income-bruno@local.invalid",
			"senha123",
			"Casa do Bruno"
		));

		String tokenAna = loginApi("income-ana@local.invalid", "senha123");
		String tokenBruno = loginApi("income-bruno@local.invalid", "senha123");

		String referenceResponse = mockMvc.perform(post("/api/v1/space/references")
				.header("Authorization", bearer(tokenBruno))
				.contentType("application/json")
				.content("""
					{
					  "type":"CLIENTE",
					  "name":"Cliente do Bruno"
					}
					"""))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();

		long foreignSpaceReferenceId = objectMapper.readTree(referenceResponse)
			.path("data")
			.path("reference")
			.path("id")
			.asLong();

		mockMvc.perform(post("/api/v1/incomes")
				.header("Authorization", bearer(tokenAna))
				.contentType("application/json")
				.content("""
					{
					  "description":"Comissão",
					  "amount":950.00,
					  "receivedOn":"2026-03-28",
					  "spaceReferenceId":%s
					}
					""".formatted(foreignSpaceReferenceId)))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("BUSINESS_RULE"))
			.andExpect(jsonPath("$.message").value("spaceReferenceId must belong to the active household"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("spaceReferenceId"));
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

		JsonNode data = objectMapper.readTree(response).path("data");
		return data.path("accessToken").asText();
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}
}
