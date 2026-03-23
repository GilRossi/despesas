package com.gilrossi.despesas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.AppUserRepository;
import com.gilrossi.despesas.identity.PlatformUserRole;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.support.ApiAuthTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
class FinancialAssistantIntegrationTest {

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

	@Autowired
	private AppUserRepository appUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deve_respeitar_isolamento_por_household_e_responder_em_fallback_sem_ia() throws Exception {
		RegistrationResponse ana = registrationService.register(new RegistrationRequest("Ana", "assistant-ana@local.invalid", "senha123", "Casa da Ana"));
		RegistrationResponse bruno = registrationService.register(new RegistrationRequest("Bruno", "assistant-bruno@local.invalid", "senha456", "Casa do Bruno"));

		Category moradiaAna = categoryRepository.save(ana.householdId(), new Category(null, "Moradia", true));
		Subcategory aluguelAna = subcategoryRepository.save(ana.householdId(), new Subcategory(null, moradiaAna.getId(), "Aluguel", true));
		expenseRepository.save(new Expense(
			ana.householdId(),
			"Aluguel março",
			new BigDecimal("900.00"),
			LocalDate.of(2026, 3, 5),
			ExpenseContext.CASA,
			moradiaAna.getId(),
			moradiaAna.getName(),
			aluguelAna.getId(),
			aluguelAna.getName(),
			null
		));

		Category transporteBruno = categoryRepository.save(bruno.householdId(), new Category(null, "Transporte", true));
		Subcategory combustivelBruno = subcategoryRepository.save(bruno.householdId(), new Subcategory(null, transporteBruno.getId(), "Combustível", true));
		expenseRepository.save(new Expense(
			bruno.householdId(),
			"Combustível março",
			new BigDecimal("300.00"),
			LocalDate.of(2026, 3, 8),
			ExpenseContext.VEICULO,
			transporteBruno.getId(),
			transporteBruno.getName(),
			combustivelBruno.getId(),
			combustivelBruno.getName(),
			null
		));

		String tokenAna = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "assistant-ana@local.invalid", "senha123");
		String tokenBruno = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "assistant-bruno@local.invalid", "senha456");

		mockMvc.perform(get("/api/v1/financial-assistant/summary")
				.header("Authorization", "Bearer " + tokenAna)
				.param("from", "2026-03-01")
				.param("to", "2026-03-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalAmount").value(900.00))
			.andExpect(jsonPath("$.data.categoryTotals[0].categoryName").value("Moradia"));

		mockMvc.perform(get("/api/v1/financial-assistant/summary")
				.header("Authorization", "Bearer " + tokenBruno)
				.param("from", "2026-03-01")
				.param("to", "2026-03-31"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.totalAmount").value(300.00))
			.andExpect(jsonPath("$.data.categoryTotals[0].categoryName").value("Transporte"));

			mockMvc.perform(post("/api/v1/financial-assistant/query")
					.header("Authorization", "Bearer " + tokenAna)
					.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question":"Onde estou gastando mais?",
					  "referenceMonth":"2026-03"
					}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.mode").value("FALLBACK"))
				.andExpect(jsonPath("$.data.highestSpendingCategory.categoryName").value("Moradia"))
				.andExpect(jsonPath("$.data.topExpenses[0].description").value("Aluguel março"))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Combustível março"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Transporte"))));
	}

	@Test
	void deve_retornar_recomendacao_de_sem_dados_quando_household_ainda_nao_tem_despesas() throws Exception {
		registrationService.register(new RegistrationRequest("Clara", "assistant-clara@local.invalid", "senha123", "Casa da Clara"));
		String tokenClara = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "assistant-clara@local.invalid", "senha123");

		mockMvc.perform(get("/api/v1/financial-assistant/recommendations")
				.header("Authorization", "Bearer " + tokenClara)
				.param("referenceMonth", "2026-03"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.recommendations[0].title").value("Sem dados suficientes"));
	}

	@Test
	void deve_manter_mes_de_referencia_atual_em_pergunta_de_aumento_vs_mes_passado() throws Exception {
		RegistrationResponse paula = registrationService.register(new RegistrationRequest("Paula", "assistant-paula@local.invalid", "senha123", "Casa da Paula"));

		Category lazer = categoryRepository.save(paula.householdId(), new Category(null, "Lazer", true));
		Subcategory passeio = subcategoryRepository.save(paula.householdId(), new Subcategory(null, lazer.getId(), "Passeio", true));
		Category moradia = categoryRepository.save(paula.householdId(), new Category(null, "Moradia", true));
		Subcategory aluguel = subcategoryRepository.save(paula.householdId(), new Subcategory(null, moradia.getId(), "Aluguel", true));

		expenseRepository.save(new Expense(
			paula.householdId(),
			"Aluguel fevereiro",
			new BigDecimal("900.00"),
			LocalDate.of(2026, 2, 5),
			ExpenseContext.CASA,
			moradia.getId(),
			moradia.getName(),
			aluguel.getId(),
			aluguel.getName(),
			null
		));
		expenseRepository.save(new Expense(
			paula.householdId(),
			"Aluguel março",
			new BigDecimal("900.00"),
			LocalDate.of(2026, 3, 5),
			ExpenseContext.CASA,
			moradia.getId(),
			moradia.getName(),
			aluguel.getId(),
			aluguel.getName(),
			null
		));
		expenseRepository.save(new Expense(
			paula.householdId(),
			"Passeio março",
			new BigDecimal("400.00"),
			LocalDate.of(2026, 3, 20),
			ExpenseContext.GERAL,
			lazer.getId(),
			lazer.getName(),
			passeio.getId(),
			passeio.getName(),
			null
		));

		String tokenPaula = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "assistant-paula@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/financial-assistant/query")
				.header("Authorization", "Bearer " + tokenPaula)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question":"O que aumentou em relação ao mês passado?",
					  "referenceMonth":"2026-03"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.mode").value("FALLBACK"))
			.andExpect(jsonPath("$.data.monthComparison.currentMonth").value("2026-03"))
				.andExpect(jsonPath("$.data.increaseAlerts[0].categoryName").value("Lazer"))
				.andExpect(jsonPath("$.data.answer").value("Lazer aumentou 400.00 em relacao ao mes anterior, um crescimento de 100.00%."));
	}

	@Test
	void deve_negar_assistente_para_platform_admin_sem_household_ativo() throws Exception {
		String adminEmail = "assistant-platform-admin@local.invalid";
		appUserRepository.save(new AppUser(
			"Platform Admin",
			adminEmail,
			passwordEncoder.encode("senha123"),
			PlatformUserRole.PLATFORM_ADMIN
		));

		String adminToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, adminEmail, "senha123");

		mockMvc.perform(post("/api/v1/financial-assistant/query")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question":"Como posso economizar este mês?",
					  "referenceMonth":"2026-03"
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.message").value("Active household membership is required for financial assistant queries"));
	}

	@Test
	void deve_nao_resolver_categoria_que_existe_apenas_em_outro_household() throws Exception {
		RegistrationResponse ana = registrationService.register(new RegistrationRequest("Ana", "assistant-scope-ana@local.invalid", "senha123", "Casa da Ana"));
		RegistrationResponse bruno = registrationService.register(new RegistrationRequest("Bruno", "assistant-scope-bruno@local.invalid", "senha456", "Casa do Bruno"));

		Category moradiaAna = categoryRepository.save(ana.householdId(), new Category(null, "Moradia", true));
		subcategoryRepository.save(ana.householdId(), new Subcategory(null, moradiaAna.getId(), "Aluguel", true));
		Category transporteBruno = categoryRepository.save(bruno.householdId(), new Category(null, "Transporte", true));
		Subcategory combustivelBruno = subcategoryRepository.save(bruno.householdId(), new Subcategory(null, transporteBruno.getId(), "Combustível", true));
		expenseRepository.save(new Expense(
			bruno.householdId(),
			"Combustível março",
			new BigDecimal("300.00"),
			LocalDate.of(2026, 3, 8),
			ExpenseContext.VEICULO,
			transporteBruno.getId(),
			transporteBruno.getName(),
			combustivelBruno.getId(),
			combustivelBruno.getName(),
			null
		));

		String tokenAna = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "assistant-scope-ana@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/financial-assistant/query")
				.header("Authorization", "Bearer " + tokenAna)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question":"Quanto gastei com transporte?",
					  "referenceMonth":"2026-03"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.intent").value("UNKNOWN"))
			.andExpect(jsonPath("$.data.answer").value("Nao consegui identificar uma intencao financeira especifica. Posso ajudar com resumo do mes, maiores gastos, comparacao entre meses, recorrencias ou economia."))
			.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Combustível março"))))
			.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Transporte"))));
	}
}
