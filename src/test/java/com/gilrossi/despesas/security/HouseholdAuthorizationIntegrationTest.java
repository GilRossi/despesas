package com.gilrossi.despesas.security;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.HouseholdMember;
import com.gilrossi.despesas.identity.HouseholdMemberRepository;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.identity.HouseholdRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;

@SpringBootTest
@AutoConfigureMockMvc
class HouseholdAuthorizationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private HouseholdMemberRepository householdMemberRepository;

	@Autowired
	private HouseholdRepository householdRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JpaCategoryRepositoryAdapter categoryRepository;

	@Autowired
	private JpaSubcategoryRepositoryAdapter subcategoryRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void deve_permitir_owner_criar_membro_via_api() throws Exception {
		registrationService.register(new RegistrationRequest("Ana", "owner-household-api@local.invalid", "senha123", "Casa Owner"));
		String ownerToken = loginApi("owner-household-api@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/household/members")
				.header("Authorization", bearer(ownerToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"name":"Bia",
						"email":"bia-household-api@local.invalid",
						"password":"senha123"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.email").value("bia-household-api@local.invalid"))
			.andExpect(jsonPath("$.data.role").value("MEMBER"));
	}

	@Test
	void deve_negar_gestao_de_household_e_catalogo_para_member() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest("Ana", "owner-member-role@local.invalid", "senha123", "Casa Role"));
		criarMembro(owner.householdId(), "Bia", "member-role@local.invalid", "senha123", HouseholdMemberRole.MEMBER);
		String memberToken = loginApi("member-role@local.invalid", "senha123");

		mockMvc.perform(get("/api/v1/household/members")
				.header("Authorization", bearer(memberToken)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		mockMvc.perform(post("/api/v1/categories")
				.header("Authorization", bearer(memberToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"Casa","active":true}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void deve_permitir_member_ler_catalogo_do_mesmo_household() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest("Ana", "owner-catalog-read@local.invalid", "senha123", "Casa Leitura"));
		criarMembro(owner.householdId(), "Bia", "member-catalog-read@local.invalid", "senha123", HouseholdMemberRole.MEMBER);
		String memberToken = loginApi("member-catalog-read@local.invalid", "senha123");

		String response = mockMvc.perform(get("/api/v1/catalog/options")
				.header("Authorization", bearer(memberToken)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		var data = objectMapper.readTree(response).path("data");
		var moradiaNode = findCategory(data, "Moradia");
		org.junit.jupiter.api.Assertions.assertNotNull(moradiaNode);
		org.junit.jupiter.api.Assertions.assertTrue(hasSubcategory(moradiaNode.path("subcategories"), "Internet"));
	}

	private String loginApi(String email, String password) throws Exception {
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
		return objectMapper.readTree(response).path("data").path("accessToken").asText();
	}

	private String bearer(String accessToken) {
		return "Bearer " + accessToken;
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

	private void criarMembro(Long householdId, String name, String email, String password, HouseholdMemberRole role) {
		AppUser user = appUserRepository.save(new AppUser(name, email, passwordEncoder.encode(password)));
		var household = householdRepository.findById(householdId).orElseThrow();
		householdMemberRepository.save(new HouseholdMember(household, user, role));
	}
}
