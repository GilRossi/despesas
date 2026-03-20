package com.gilrossi.despesas.api.v1.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogOptionsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private JpaCategoryRepositoryAdapter categoryRepository;

	@Autowired
	private JpaSubcategoryRepositoryAdapter subcategoryRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void deve_listar_apenas_catalogo_ativo_do_household_autenticado() throws Exception {
		RegistrationResponse ana = registrationService.register(new RegistrationRequest("Ana", "ana-catalog@local.invalid", "senha123", "Casa Ana"));
		RegistrationResponse bruno = registrationService.register(new RegistrationRequest("Bruno", "bruno-catalog@local.invalid", "senha123", "Casa Bruno"));

		Category moradia = categoryRepository.save(ana.householdId(), new Category(null, "Moradia", true));
		subcategoryRepository.save(ana.householdId(), new Subcategory(null, moradia.getId(), "Internet", true));
		subcategoryRepository.save(ana.householdId(), new Subcategory(null, moradia.getId(), "Mercado", true));

		Category lazer = categoryRepository.save(bruno.householdId(), new Category(null, "Lazer", true));
		subcategoryRepository.save(bruno.householdId(), new Subcategory(null, lazer.getId(), "Cinema", true));
		String accessToken = loginApi("ana-catalog@local.invalid", "senha123");

		String response = mockMvc.perform(get("/api/v1/catalog/options")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		var data = objectMapper.readTree(response).path("data");
		var moradiaNode = findCategory(data, "Moradia");
		org.junit.jupiter.api.Assertions.assertNotNull(moradiaNode);
		org.junit.jupiter.api.Assertions.assertTrue(hasSubcategory(moradiaNode.path("subcategories"), "Internet"));
		org.junit.jupiter.api.Assertions.assertTrue(hasSubcategory(moradiaNode.path("subcategories"), "Mercado"));
		org.junit.jupiter.api.Assertions.assertNull(findCategory(data, "Lazer"));
	}

	private com.fasterxml.jackson.databind.JsonNode findCategory(com.fasterxml.jackson.databind.JsonNode categories, String name) {
		for (var category : categories) {
			if (name.equals(category.path("name").asText())) {
				return category;
			}
		}
		return null;
	}

	private boolean hasSubcategory(com.fasterxml.jackson.databind.JsonNode subcategories, String name) {
		for (var subcategory : subcategories) {
			if (name.equals(subcategory.path("name").asText())) {
				return true;
			}
		}
		return false;
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
}
