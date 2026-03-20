package com.gilrossi.despesas.security;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
class HouseholdIsolationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JpaCategoryRepositoryAdapter categoryRepository;

	@Autowired
	private JpaSubcategoryRepositoryAdapter subcategoryRepository;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Test
	void deve_resolver_household_correto_para_cada_usuario_na_api_me() throws Exception {
		registrationService.register(new RegistrationRequest("Ana", "ana-household@local.invalid", "senha123", "Casa da Ana"));
		registrationService.register(new RegistrationRequest("Bruno", "bruno-household@local.invalid", "senha456", "Casa do Bruno"));

		String tokenAna = loginApi("ana-household@local.invalid", "senha123");
		String tokenBruno = loginApi("bruno-household@local.invalid", "senha456");

		String responseAna = mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", bearer(tokenAna)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.email").value("ana-household@local.invalid"))
			.andExpect(jsonPath("$.data.role").value("OWNER"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		String responseBruno = mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", bearer(tokenBruno)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.email").value("bruno-household@local.invalid"))
			.andExpect(jsonPath("$.data.role").value("OWNER"))
			.andExpect(jsonPath("$.data.householdId").exists())
			.andReturn()
			.getResponse()
			.getContentAsString();

		JsonNode ana = objectMapper.readTree(responseAna);
		JsonNode bruno = objectMapper.readTree(responseBruno);
		assertNotEquals(ana.get("data").get("householdId").asLong(), bruno.get("data").get("householdId").asLong());
	}

	@Test
	void deve_permitir_membro_do_mesmo_household_acessar_despesa_e_isolar_outro_household() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest("Ana", "ana-owner@local.invalid", "senha123", "Casa da Ana"));
		registrationService.register(new RegistrationRequest("Bruno", "bruno-owner@local.invalid", "senha456", "Casa do Bruno"));
		String ownerToken = loginApi("ana-owner@local.invalid", "senha123");
		String memberlessOtherToken = loginApi("bruno-owner@local.invalid", "senha456");

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/household/members")
				.header("Authorization", bearer(ownerToken))
				.contentType("application/json")
				.content("""
					{
					  "name":"Bia",
					  "email":"bia-member@local.invalid",
					  "password":"senha789",
					  "role":"MEMBER"
					}
					"""))
			.andExpect(status().isCreated());

		Category category = categoryRepository.save(owner.householdId(), new Category(null, "Casa", true));
		Subcategory subcategory = subcategoryRepository.save(owner.householdId(), new Subcategory(null, category.getId(), "Internet", true));
		Expense expense = expenseRepository.save(new Expense(
			owner.householdId(),
			"Internet da casa",
			new BigDecimal("120.00"),
			LocalDate.now().plusDays(5),
			ExpenseContext.CASA,
			category.getId(),
			category.getName(),
			subcategory.getId(),
			subcategory.getName(),
			"Conta fixa"
		));

		String memberToken = loginApi("bia-member@local.invalid", "senha789");

		mockMvc.perform(get("/api/v1/expenses/" + expense.getId())
				.header("Authorization", bearer(memberToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(expense.getId()));

		mockMvc.perform(get("/api/v1/expenses/" + expense.getId())
				.header("Authorization", bearer(memberlessOtherToken)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	private String loginApi(String email, String password) throws Exception {
		String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
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
