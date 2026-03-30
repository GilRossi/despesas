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
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.fixedbill.FixedBill;
import com.gilrossi.despesas.fixedbill.FixedBillFrequency;
import com.gilrossi.despesas.fixedbill.FixedBillRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;

@SpringBootTest
@AutoConfigureMockMvc
class FixedBillIntegrationTest {

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
	private FixedBillRepository fixedBillRepository;

	@Test
	void deve_criar_conta_fixa_com_referencia_opcional_valida_e_persistir_no_household_correto() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest(
			"Ana",
			"fixed-bill-owner@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		String token = loginApi("fixed-bill-owner@local.invalid", "senha123");

		Category category = requireCategory(owner.householdId(), "Moradia");
		Subcategory subcategory = requireSubcategory(owner.householdId(), category.getId(), "Internet");

		String referenceResponse = mockMvc.perform(post("/api/v1/space/references")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "type":"CASA",
					  "name":"Apartamento Centro"
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

		String createResponse = mockMvc.perform(post("/api/v1/fixed-bills")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "description":"Internet fibra",
					  "amount":129.90,
					  "firstDueDate":"2026-04-10",
					  "frequency":"MONTHLY",
					  "categoryId":%s,
					  "subcategoryId":%s,
					  "spaceReferenceId":%s
					}
					""".formatted(category.getId(), subcategory.getId(), spaceReferenceId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.description").value("Internet fibra"))
			.andExpect(jsonPath("$.data.amount").value(129.90))
			.andExpect(jsonPath("$.data.firstDueDate").value("2026-04-10"))
			.andExpect(jsonPath("$.data.frequency").value("MONTHLY"))
			.andExpect(jsonPath("$.data.category.id").value(category.getId()))
			.andExpect(jsonPath("$.data.category.name").value("Moradia"))
			.andExpect(jsonPath("$.data.subcategory.id").value(subcategory.getId()))
			.andExpect(jsonPath("$.data.subcategory.name").value("Internet"))
			.andExpect(jsonPath("$.data.spaceReference.id").value(spaceReferenceId))
			.andExpect(jsonPath("$.data.spaceReference.name").value("Apartamento Centro"))
			.andExpect(jsonPath("$.data.active").value(true))
			.andReturn()
			.getResponse()
			.getContentAsString();

		long fixedBillId = objectMapper.readTree(createResponse).path("data").path("id").asLong();
		FixedBill persisted = fixedBillRepository.findById(fixedBillId).orElseThrow();

		assertThat(persisted.getHouseholdId()).isEqualTo(owner.householdId());
		assertThat(persisted.getDescription()).isEqualTo("Internet fibra");
		assertThat(persisted.getAmount()).isEqualByComparingTo(new BigDecimal("129.90"));
		assertThat(persisted.getFirstDueDate()).isEqualTo(LocalDate.of(2026, 4, 10));
		assertThat(persisted.getFrequency()).isEqualTo(FixedBillFrequency.MONTHLY);
		assertThat(persisted.getContext()).isEqualTo(ExpenseContext.GERAL);
		assertThat(persisted.getCategoryId()).isEqualTo(category.getId());
		assertThat(persisted.getSubcategoryId()).isEqualTo(subcategory.getId());
		assertThat(persisted.getSpaceReferenceId()).isEqualTo(spaceReferenceId);
		assertThat(persisted.isActive()).isTrue();
		assertThat(persisted.getCreatedAt()).isNotNull();
	}

	@Test
	void deve_rejeitar_categoria_de_outro_household_ao_criar_conta_fixa() throws Exception {
		RegistrationResponse ana = registrationService.register(new RegistrationRequest(
			"Ana",
			"fixed-bill-ana@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		RegistrationResponse bruno = registrationService.register(new RegistrationRequest(
			"Bruno",
			"fixed-bill-bruno@local.invalid",
			"senha123",
			"Casa do Bruno"
		));

		String tokenAna = loginApi("fixed-bill-ana@local.invalid", "senha123");
		Category foreignCategory = requireCategory(bruno.householdId(), "Moradia");
		Subcategory foreignSubcategory = requireSubcategory(bruno.householdId(), foreignCategory.getId(), "Internet");

		mockMvc.perform(post("/api/v1/fixed-bills")
				.header("Authorization", bearer(tokenAna))
				.contentType("application/json")
				.content("""
					{
					  "description":"Condomínio",
					  "amount":450.00,
					  "firstDueDate":"2026-04-05",
					  "frequency":"MONTHLY",
					  "categoryId":%s,
					  "subcategoryId":%s
					}
					""".formatted(foreignCategory.getId(), foreignSubcategory.getId())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("Category with id " + foreignCategory.getId() + " was not found"));
	}

	@Test
	void deve_rejeitar_subcategoria_invalida_ao_criar_conta_fixa() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest(
			"Ana",
			"fixed-bill-invalid-sub@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		String token = loginApi("fixed-bill-invalid-sub@local.invalid", "senha123");
		Category category = requireCategory(owner.householdId(), "Moradia");

		mockMvc.perform(post("/api/v1/fixed-bills")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "description":"Condomínio",
					  "amount":450.00,
					  "firstDueDate":"2026-04-05",
					  "frequency":"MONTHLY",
					  "categoryId":%s,
					  "subcategoryId":999999
					}
					""".formatted(category.getId())))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("Subcategory with id 999999 was not found"));
	}

	@Test
	void deve_rejeitar_subcategoria_incompativel_com_a_categoria() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest(
			"Ana",
			"fixed-bill-incompatible@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		String token = loginApi("fixed-bill-incompatible@local.invalid", "senha123");
		Category category = requireCategory(owner.householdId(), "Moradia");
		Category otherCategory = requireCategory(owner.householdId(), "Alimentação");
		Subcategory incompatibleSubcategory = requireSubcategory(owner.householdId(), otherCategory.getId(), "Mercado");

		mockMvc.perform(post("/api/v1/fixed-bills")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "description":"Condomínio",
					  "amount":450.00,
					  "firstDueDate":"2026-04-05",
					  "frequency":"MONTHLY",
					  "categoryId":%s,
					  "subcategoryId":%s
					}
					""".formatted(category.getId(), incompatibleSubcategory.getId())))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("BUSINESS_RULE"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("subcategoryId"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("subcategoryId must belong to the informed category"));
	}

	@Test
	void deve_rejeitar_referencia_de_outro_household_ao_criar_conta_fixa() throws Exception {
		RegistrationResponse ana = registrationService.register(new RegistrationRequest(
			"Ana",
			"fixed-bill-ref-ana@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		RegistrationResponse bruno = registrationService.register(new RegistrationRequest(
			"Bruno",
			"fixed-bill-ref-bruno@local.invalid",
			"senha123",
			"Casa do Bruno"
		));

		String tokenAna = loginApi("fixed-bill-ref-ana@local.invalid", "senha123");
		String tokenBruno = loginApi("fixed-bill-ref-bruno@local.invalid", "senha123");

		Category category = requireCategory(ana.householdId(), "Moradia");
		Subcategory subcategory = requireSubcategory(ana.householdId(), category.getId(), "Internet");

		String referenceResponse = mockMvc.perform(post("/api/v1/space/references")
				.header("Authorization", bearer(tokenBruno))
				.contentType("application/json")
				.content("""
					{
					  "type":"CASA",
					  "name":"Casa do Bruno"
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

		mockMvc.perform(post("/api/v1/fixed-bills")
				.header("Authorization", bearer(tokenAna))
				.contentType("application/json")
				.content("""
					{
					  "description":"Internet fibra",
					  "amount":129.90,
					  "firstDueDate":"2026-04-10",
					  "frequency":"MONTHLY",
					  "categoryId":%s,
					  "subcategoryId":%s,
					  "spaceReferenceId":%s
					}
					""".formatted(category.getId(), subcategory.getId(), foreignSpaceReferenceId)))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("BUSINESS_RULE"))
			.andExpect(jsonPath("$.message").value("spaceReferenceId must belong to the active household"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("spaceReferenceId"));
	}

	@Test
	void deve_rejeitar_frequency_diferente_de_monthly() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest(
			"Ana",
			"fixed-bill-frequency@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		String token = loginApi("fixed-bill-frequency@local.invalid", "senha123");
		Category category = requireCategory(owner.householdId(), "Moradia");
		Subcategory subcategory = requireSubcategory(owner.householdId(), category.getId(), "Internet");

		mockMvc.perform(post("/api/v1/fixed-bills")
				.header("Authorization", bearer(token))
				.contentType("application/json")
				.content("""
					{
					  "description":"Internet fibra",
					  "amount":129.90,
					  "firstDueDate":"2026-04-10",
					  "frequency":"WEEKLY",
					  "categoryId":%s,
					  "subcategoryId":%s
					}
					""".formatted(category.getId(), subcategory.getId())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("Request payload is invalid"));
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
