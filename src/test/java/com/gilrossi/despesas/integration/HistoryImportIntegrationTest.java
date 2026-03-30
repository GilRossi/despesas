package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.payment.Payment;
import com.gilrossi.despesas.payment.PaymentRepository;

@SpringBootTest
@AutoConfigureMockMvc
class HistoryImportIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private SubcategoryRepository subcategoryRepository;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Test
	void deve_importar_lote_de_historico_e_criar_expenses_e_pagamentos_no_household_correto() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest(
			"Ana",
			"history-import-owner@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		String token = loginApi("history-import-owner@local.invalid", "senha123");

		Category category = requireCategory(owner.householdId(), "Moradia");
		Subcategory subcategory = requireSubcategory(owner.householdId(), category.getId(), "Internet");

		String response = mockMvc.perform(post("/api/v1/history-imports")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "paymentMethod":"PIX",
					  "entries":[
					    {
					      "description":"Internet janeiro",
					      "amount":129.90,
					      "date":"2026-01-10",
					      "categoryId":%s,
					      "subcategoryId":%s,
					      "notes":"Historico importado de janeiro"
					    },
					    {
					      "description":"Internet fevereiro",
					      "amount":129.90,
					      "date":"2026-02-10",
					      "categoryId":%s,
					      "subcategoryId":%s
					    }
					  ]
					}
					""".formatted(category.getId(), subcategory.getId(), category.getId(), subcategory.getId())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.importedCount").value(2))
			.andExpect(jsonPath("$.data.entries[0].status").value("PAGA"))
			.andExpect(jsonPath("$.data.entries[1].status").value("PAGA"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		JsonNode data = objectMapper.readTree(response).path("data");
		List<Long> expenseIds = List.of(
			data.path("entries").get(0).path("expenseId").asLong(),
			data.path("entries").get(1).path("expenseId").asLong()
		);

		List<Expense> expenses = expenseRepository.findAllByHouseholdId(owner.householdId());
		assertThat(expenses).hasSize(2);
		assertThat(expenses)
			.extracting(Expense::getDescription)
			.containsExactlyInAnyOrder("Internet janeiro", "Internet fevereiro");

		List<Payment> payments = paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(expenseIds);
		assertThat(payments).hasSize(2);
		assertThat(payments)
			.extracting(payment -> payment.getMethod().name())
			.containsOnly("PIX");
	}

	@Test
	void deve_manter_all_or_nothing_quando_uma_entry_do_lote_for_invalida() throws Exception {
		RegistrationResponse ana = registrationService.register(new RegistrationRequest(
			"Ana",
			"history-import-rollback@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		RegistrationResponse bruno = registrationService.register(new RegistrationRequest(
			"Bruno",
			"history-import-rollback-bruno@local.invalid",
			"senha123",
			"Casa do Bruno"
		));
		String tokenAna = loginApi("history-import-rollback@local.invalid", "senha123");

		Category validCategory = requireCategory(ana.householdId(), "Moradia");
		Subcategory validSubcategory = requireSubcategory(ana.householdId(), validCategory.getId(), "Internet");
		Category foreignCategory = requireCategory(bruno.householdId(), "Moradia");
		Subcategory foreignSubcategory = requireSubcategory(bruno.householdId(), foreignCategory.getId(), "Internet");

		mockMvc.perform(post("/api/v1/history-imports")
				.header("Authorization", bearer(tokenAna))
				.contentType("application/json")
				.content("""
					{
					  "paymentMethod":"PIX",
					  "entries":[
					    {
					      "description":"Internet janeiro",
					      "amount":129.90,
					      "date":"2026-01-10",
					      "categoryId":%s,
					      "subcategoryId":%s
					    },
					    {
					      "description":"Internet fevereiro",
					      "amount":129.90,
					      "date":"2026-02-10",
					      "categoryId":%s,
					      "subcategoryId":%s
					    }
					  ]
					}
					""".formatted(validCategory.getId(), validSubcategory.getId(), foreignCategory.getId(), foreignSubcategory.getId())))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("BUSINESS_RULE"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("entries[1].categoryId"));

		assertThat(expenseRepository.findAllByHouseholdId(ana.householdId())).isEmpty();
	}

	@Test
	void deve_rejeitar_categoria_de_outro_household_no_lote() throws Exception {
		RegistrationResponse ana = registrationService.register(new RegistrationRequest(
			"Ana",
			"history-import-boundary-ana@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		RegistrationResponse bruno = registrationService.register(new RegistrationRequest(
			"Bruno",
			"history-import-boundary-bruno@local.invalid",
			"senha123",
			"Casa do Bruno"
		));
		String tokenAna = loginApi("history-import-boundary-ana@local.invalid", "senha123");

		Category foreignCategory = requireCategory(bruno.householdId(), "Moradia");
		Subcategory foreignSubcategory = requireSubcategory(bruno.householdId(), foreignCategory.getId(), "Internet");

		mockMvc.perform(post("/api/v1/history-imports")
				.header("Authorization", bearer(tokenAna))
				.contentType("application/json")
				.content("""
					{
					  "paymentMethod":"PIX",
					  "entries":[
					    {
					      "description":"Historico estranho",
					      "amount":129.90,
					      "date":"2026-02-10",
					      "categoryId":%s,
					      "subcategoryId":%s
					    }
					  ]
					}
					""".formatted(foreignCategory.getId(), foreignSubcategory.getId())))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("entries[0].categoryId"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("categoryId must belong to the active household"));
	}

	@Test
	void deve_rejeitar_subcategoria_invalida_no_lote() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest(
			"Ana",
			"history-import-invalid-sub@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		String token = loginApi("history-import-invalid-sub@local.invalid", "senha123");
		Category category = requireCategory(owner.householdId(), "Moradia");

		mockMvc.perform(post("/api/v1/history-imports")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "paymentMethod":"PIX",
					  "entries":[
					    {
					      "description":"Internet janeiro",
					      "amount":129.90,
					      "date":"2026-01-10",
					      "categoryId":%s,
					      "subcategoryId":999999
					    }
					  ]
					}
					""".formatted(category.getId())))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("entries[0].subcategoryId"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("subcategoryId must belong to the active household"));
	}

	@Test
	void deve_rejeitar_subcategoria_incompativel_com_a_categoria_no_lote() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest(
			"Ana",
			"history-import-incompatible@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		String token = loginApi("history-import-incompatible@local.invalid", "senha123");
		Category category = requireCategory(owner.householdId(), "Moradia");
		Category otherCategory = requireCategory(owner.householdId(), "Alimentação");
		Subcategory incompatibleSubcategory = requireSubcategory(owner.householdId(), otherCategory.getId(), "Mercado");

		mockMvc.perform(post("/api/v1/history-imports")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "paymentMethod":"PIX",
					  "entries":[
					    {
					      "description":"Internet janeiro",
					      "amount":129.90,
					      "date":"2026-01-10",
					      "categoryId":%s,
					      "subcategoryId":%s
					    }
					  ]
					}
					""".formatted(category.getId(), incompatibleSubcategory.getId())))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("entries[0].subcategoryId"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("subcategoryId must belong to the informed category"));
	}

	private Category requireCategory(Long householdId, String name) {
		return categoryRepository.findByNameIgnoreCase(householdId, name).orElseThrow();
	}

	private Subcategory requireSubcategory(Long householdId, Long categoryId, String name) {
		return subcategoryRepository.findActiveByHouseholdId(householdId).stream()
			.filter(subcategory -> subcategory.getCategoryId().equals(categoryId))
			.filter(subcategory -> subcategory.getName().equalsIgnoreCase(name))
			.findFirst()
			.orElseThrow();
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
