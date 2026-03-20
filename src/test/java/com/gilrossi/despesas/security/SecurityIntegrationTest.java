package com.gilrossi.despesas.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

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

	@Test
	void deve_redirecionar_para_login_quando_web_sem_autenticacao() throws Exception {
		mockMvc.perform(get("/despesas"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("http://localhost/login"));
	}

	@Test
	void deve_redirecionar_raiz_autenticada_para_lista_de_despesas() throws Exception {
		mockMvc.perform(get("/")
				.with(user("ana@local.invalid").roles("OWNER")))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/despesas"));
	}

	@Test
	void deve_retornar_401_quando_api_sem_autenticacao() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void deve_bloquear_post_web_sem_csrf() throws Exception {
		RegistrationResponse registration = registrationService.register(new RegistrationRequest(
			"Ana",
			"ana-web-sem-csrf@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		List<Category> categories = categoryRepository.findActiveByHouseholdId(registration.householdId());
		Category category = categories.getFirst();
		Subcategory subcategory = subcategoryRepository.findActiveByHouseholdId(registration.householdId()).stream()
			.filter(item -> item.getCategoryId().equals(category.getId()))
			.findFirst()
			.orElseThrow();

		mockMvc.perform(post("/despesas/salvar")
				.with(user(ownerPrincipal(registration)))
				.param("descricao", "Internet")
				.param("valor", "120.00")
				.param("data", "2026-03-19")
				.param("contexto", "CASA")
				.param("categoriaId", category.getId().toString())
				.param("subcategoriaId", subcategory.getId().toString()))
			.andExpect(status().isForbidden());
	}

	@Test
	void deve_aceitar_post_web_com_csrf() throws Exception {
		RegistrationResponse registration = registrationService.register(new RegistrationRequest(
			"Ana",
			"ana-web-form@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		List<Category> categories = categoryRepository.findActiveByHouseholdId(registration.householdId());
		Category category = categories.getFirst();
		Subcategory subcategory = subcategoryRepository.findActiveByHouseholdId(registration.householdId()).stream()
			.filter(item -> item.getCategoryId().equals(category.getId()))
			.findFirst()
			.orElseThrow();

		mockMvc.perform(post("/despesas/salvar")
				.with(user(ownerPrincipal(registration)))
				.with(csrf())
				.param("descricao", "Internet")
				.param("valor", "120.00")
				.param("data", "2026-03-19")
				.param("contexto", "CASA")
				.param("categoriaId", category.getId().toString())
				.param("subcategoriaId", subcategory.getId().toString()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/despesas"));
	}

	@Test
	void deve_rejeitar_basic_auth_em_endpoints_da_api() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"ana@local.invalid",
			"senha123",
			"Casa da Ana"
		));

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Basic YW5hQGxvY2FsLmludmFsaWQ6c2VuaGExMjM="))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void deve_autenticar_api_via_login_e_consultar_me_com_bearer_token() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"ana-token@local.invalid",
			"senha123",
			"Casa da Ana"
		));

		String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
				.contentType("application/json")
				.content("""
					{
					  "email":"ana-token@local.invalid",
					  "password":"senha123"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.accessToken").isString())
			.andExpect(jsonPath("$.data.refreshToken").isString())
			.andExpect(jsonPath("$.data.user.email").value("ana-token@local.invalid"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.email").value("ana-token@local.invalid"))
			.andExpect(jsonPath("$.data.householdId").value(greaterThanOrEqualTo(1)));
	}

	@Test
	void deve_renovar_token_via_refresh() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"ana-refresh@local.invalid",
			"senha123",
			"Casa da Ana"
		));

		String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
				.contentType("application/json")
				.content("""
					{
					  "email":"ana-refresh@local.invalid",
					  "password":"senha123"
					}
					"""))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		String refreshToken = objectMapper.readTree(loginResponse).path("data").path("refreshToken").asText();

		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType("application/json")
				.content("""
					{
					  "refreshToken":"%s"
					}
					""".formatted(refreshToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.accessToken").isString())
			.andExpect(jsonPath("$.data.refreshToken").isString())
			.andExpect(jsonPath("$.data.user.email").value("ana-refresh@local.invalid"));
	}

	@Test
	void deve_retornar_401_com_envelope_padronizado_quando_login_api_for_invalido() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"ana-login-api@local.invalid",
			"senha123",
			"Casa da Ana"
		));

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType("application/json")
				.content("""
					{
					  "email":"ana-login-api@local.invalid",
					  "password":"senha-errada"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void deve_retornar_401_com_envelope_padronizado_quando_token_for_invalido() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer token-invalido"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void deve_autenticar_login_web_com_credenciais_validas() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"ana-login@local.invalid",
			"senha123",
			"Casa da Ana"
		));

		mockMvc.perform(post("/login")
				.with(csrf())
				.param("username", "ana-login@local.invalid")
				.param("password", "senha123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/despesas"))
			.andExpect(authenticated().withUsername("ana-login@local.invalid"));
	}

	@Test
	void deve_rejeitar_login_web_com_credenciais_invalidas() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"ana-login-invalido@local.invalid",
			"senha123",
			"Casa da Ana"
		));

		mockMvc.perform(post("/login")
				.with(csrf())
				.param("username", "ana-login-invalido@local.invalid")
				.param("password", "senha-errada"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login?error"))
			.andExpect(unauthenticated());
	}

	@Test
	void deve_registrar_usuario_e_household_via_api() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType("application/json")
				.content("""
					{
					  "name":"Carla",
					  "email":"carla@local.invalid",
					  "password":"senha123",
					  "householdName":"Casa da Carla"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.email").value("carla@local.invalid"))
			.andExpect(jsonPath("$.data.role").value("OWNER"))
			.andExpect(jsonPath("$.data.householdId").value(greaterThanOrEqualTo(1)));
	}

	@Test
	void deve_retornar_409_quando_registro_duplicado_via_api() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType("application/json")
				.content("""
					{
					  "name":"Carla",
					  "email":"carla-duplicada@local.invalid",
					  "password":"senha123",
					  "householdName":"Casa da Carla"
					}
					"""))
			.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType("application/json")
				.content("""
					{
					  "name":"Carla Outra",
					  "email":"carla-duplicada@local.invalid",
					  "password":"senha123",
					  "householdName":"Casa da Carla Outra"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("CONFLICT"));
	}

	@Test
	void deve_permitir_owner_criar_membro_no_household() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"owner-household@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		String accessToken = loginApi("owner-household@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/household/members")
				.header("Authorization", bearer(accessToken))
				.contentType("application/json")
				.content("""
					{
					  "name":"Bia",
					  "email":"bia-household@local.invalid",
					  "password":"senha456",
					  "role":"MEMBER"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.email").value("bia-household@local.invalid"))
			.andExpect(jsonPath("$.data.role").value("MEMBER"));
	}

	@Test
	void deve_negor_mutacao_de_catalogo_para_member() throws Exception {
		registrationService.register(new RegistrationRequest(
			"Ana",
			"owner-catalog@local.invalid",
			"senha123",
			"Casa da Ana"
		));
		String ownerToken = loginApi("owner-catalog@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/household/members")
				.header("Authorization", bearer(ownerToken))
				.contentType("application/json")
				.content("""
					{
					  "name":"Bia",
					  "email":"member-catalog@local.invalid",
					  "password":"senha456",
					  "role":"MEMBER"
					}
					"""))
			.andExpect(status().isCreated());
		String memberToken = loginApi("member-catalog@local.invalid", "senha456");

		mockMvc.perform(post("/api/v1/categories")
				.header("Authorization", bearer(memberToken))
				.contentType("application/json")
				.content("""
					{
					  "name":"Casa",
					  "active":true
					}
				"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"))
			.andExpect(jsonPath("$.fieldErrors").isArray())
			.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void deve_bootstrapar_catalogo_inicial_e_permitir_primeira_despesa_web_para_household_novo() throws Exception {
		RegistrationResponse registration = registrationService.register(new RegistrationRequest(
			"Ana",
			"ana-onboarding@local.invalid",
			"senha123",
			"Casa Onboarding"
		));

		List<Category> categories = categoryRepository.findActiveByHouseholdId(registration.householdId());
		List<Subcategory> subcategories = subcategoryRepository.findActiveByHouseholdId(registration.householdId());

		org.junit.jupiter.api.Assertions.assertFalse(categories.isEmpty());
		org.junit.jupiter.api.Assertions.assertFalse(subcategories.isEmpty());

		Category category = categories.getFirst();
		Subcategory subcategory = subcategories.stream()
			.filter(item -> item.getCategoryId().equals(category.getId()))
			.findFirst()
			.orElseThrow();

		MvcResult login = mockMvc.perform(post("/login")
				.with(csrf())
				.param("username", "ana-onboarding@local.invalid")
				.param("password", "senha123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/despesas"))
			.andReturn();

		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

		mockMvc.perform(get("/despesas/nova").session(session))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Selecione a categoria")));

		mockMvc.perform(post("/despesas/salvar")
				.session(session)
				.with(csrf())
				.param("descricao", "Primeira despesa")
				.param("valor", "89.90")
				.param("data", "2026-03-19")
				.param("contexto", "CASA")
				.param("categoriaId", category.getId().toString())
				.param("subcategoriaId", subcategory.getId().toString()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/despesas"));
	}

	private AuthenticatedHouseholdUser ownerPrincipal(RegistrationResponse registration) {
		return new AuthenticatedHouseholdUser(
			registration.userId(),
			registration.householdId(),
			HouseholdMemberRole.OWNER,
			registration.name(),
			registration.email(),
			"senha123"
		);
	}

	private Long criarCategoria(String email, String password, String name) throws Exception {
		String accessToken = loginApi(email, password);
		String response = mockMvc.perform(post("/api/v1/categories")
				.header("Authorization", bearer(accessToken))
				.contentType("application/json")
				.content("""
					{
					  "name":"%s",
					  "active":true
					}
					""".formatted(name)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(response).path("data").path("id").asLong();
	}

	private Long criarSubcategoria(String email, String password, Long categoryId, String name) throws Exception {
		String accessToken = loginApi(email, password);
		String response = mockMvc.perform(post("/api/v1/subcategories")
				.header("Authorization", bearer(accessToken))
				.contentType("application/json")
				.content("""
					{
					  "categoryId":%s,
					  "name":"%s",
					  "active":true
					}
					""".formatted(categoryId, name)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(response).path("data").path("id").asLong();
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
