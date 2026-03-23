package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.audit.PersistedAuditEvent;
import com.gilrossi.despesas.audit.PersistedAuditEventRepository;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.ratelimit.RateLimitCounterRepository;
import com.gilrossi.despesas.support.ApiAuthTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	"app.rate-limits.assistant-query.max-requests=1",
	"app.rate-limits.assistant-query.window-seconds=300"
})
class FinancialAssistantRateLimitIntegrationTest {

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
	private PersistedAuditEventRepository persistedAuditEventRepository;

	@Autowired
	private RateLimitCounterRepository rateLimitCounterRepository;

	@BeforeEach
	void setUp() {
		persistedAuditEventRepository.deleteAll();
		rateLimitCounterRepository.deleteAll();
	}

	@Test
	void deve_limitar_consultas_do_assistente_e_persistir_evento_sem_vazar_pergunta() throws Exception {
		RegistrationResponse owner = registrationService.register(new RegistrationRequest("Ana", "assistant-rate@local.invalid", "senha123", "Casa Rate"));
		Category moradia = categoryRepository.save(owner.householdId(), new Category(null, "Moradia", true));
		Subcategory aluguel = subcategoryRepository.save(owner.householdId(), new Subcategory(null, moradia.getId(), "Aluguel", true));
		expenseRepository.save(new Expense(
			owner.householdId(),
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

		String accessToken = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "assistant-rate@local.invalid", "senha123");
		String question = "Onde estou gastando mais?";

		query(accessToken, question)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.mode").value("FALLBACK"));

		query(accessToken, question)
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(jsonPath("$.code").value("RATE_LIMITED"));

		assertThat(persistedAuditEventRepository.findAll())
			.extracting(PersistedAuditEvent::getEventType)
			.contains("assistant_query_completed", "assistant_query_rate_limited");

		PersistedAuditEvent limitedEvent = persistedAuditEventRepository.findAllByEventTypeOrderByIdAsc("assistant_query_rate_limited").get(0);
		assertThat(limitedEvent.getSafeContextJson()).doesNotContain(question);
		assertThat(limitedEvent.getPrimaryReference()).isEqualTo("2026-03");
	}

	private org.springframework.test.web.servlet.ResultActions query(String accessToken, String question) throws Exception {
		return mockMvc.perform(post("/api/v1/financial-assistant/query")
			.header("Authorization", "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "question":"%s",
				  "referenceMonth":"2026-03"
				}
				""".formatted(question)));
	}
}
