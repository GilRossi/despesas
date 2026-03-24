package com.gilrossi.despesas.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationService;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	"spring.web.resources.static-locations=classpath:/flutter-web/,classpath:/static/",
	"app.security.cors-allowed-origin-patterns=http://localhost:*,http://127.0.0.1:*"
})
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void deve_servir_front_door_do_flutter_sem_autenticacao() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(forwardedUrl("index.html"));
	}

	@Test
	void deve_servir_index_do_flutter_sem_autenticacao() throws Exception {
		mockMvc.perform(get("/index.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"))
			.andExpect(content().string(containsString("despesas flutter web frontdoor")));
	}

	@Test
	void deve_servir_ativo_do_flutter_sem_autenticacao() throws Exception {
		mockMvc.perform(get("/flutter_bootstrap.js"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/javascript"))
			.andExpect(content().string(containsString("flutter frontdoor bootstrap")));
	}

	@Test
	void deve_expor_health_sem_autenticacao() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void deve_negar_post_no_login_web_legado_apos_cutover() throws Exception {
		mockMvc.perform(post("/login"))
			.andExpect(status().isForbidden());
	}

	@Test
	void deve_negar_post_em_rota_web_legada_apos_cutover() throws Exception {
		mockMvc.perform(post("/despesas/salvar"))
			.andExpect(status().isForbidden());
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
	void deve_responder_preflight_cors_para_login_da_api() throws Exception {
		mockMvc.perform(options("/api/v1/auth/login")
				.header("Origin", "http://localhost:54721")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "content-type"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:54721"))
			.andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
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
	void deve_negar_signup_publico_via_api() throws Exception {
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
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void deve_manter_signup_publico_inacessivel_mesmo_com_payload_duplicado() throws Exception {
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
			.andExpect(status().isUnauthorized());

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
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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
					  "password":"senha456"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.email").value("bia-household@local.invalid"))
			.andExpect(jsonPath("$.data.role").value("MEMBER"));
	}

	@Test
	void deve_negar_mutacao_de_catalogo_para_member() throws Exception {
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
					  "password":"senha456"
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
