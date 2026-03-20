package com.gilrossi.despesas.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.JpaSubcategoryRepositoryAdapter;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantConversationGateway;
import com.gilrossi.despesas.financialassistant.ai.FinancialAssistantGatewayException;
import com.gilrossi.despesas.identity.RegistrationRequest;
import com.gilrossi.despesas.identity.RegistrationResponse;
import com.gilrossi.despesas.identity.RegistrationService;
import com.gilrossi.despesas.support.ApiAuthTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
class FinancialAssistantQueryContractIntegrationTest {

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

	@MockitoBean
	private FinancialAssistantConversationGateway conversationGateway;

	@Test
	void deve_responder_200_em_fallback_sem_expor_erro_cru_do_provedor() throws Exception {
		RegistrationResponse maria = registrationService.register(new RegistrationRequest("Maria", "assistant-contract@local.invalid", "senha123", "Casa da Maria"));

		Category moradia = categoryRepository.save(maria.householdId(), new Category(null, "Moradia", true));
		Subcategory aluguel = subcategoryRepository.save(maria.householdId(), new Subcategory(null, moradia.getId(), "Aluguel", true));
		expenseRepository.save(new Expense(
			maria.householdId(),
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

		when(conversationGateway.isAvailable()).thenReturn(true);
		when(conversationGateway.answer(any())).thenThrow(FinancialAssistantGatewayException.from(
			new ProviderAuthenticationException(
				"{\"error\":{\"message\":\"Authentication Fails, Your api key is invalid\",\"type\":\"authentication_error\"}}"
			)
		));

		String tokenMaria = ApiAuthTestSupport.accessToken(mockMvc, objectMapper, "assistant-contract@local.invalid", "senha123");

		mockMvc.perform(post("/api/v1/financial-assistant/query")
				.header("Authorization", "Bearer " + tokenMaria)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "question":"Como posso economizar este mês?",
					  "referenceMonth":"2026-03"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.mode").value("FALLBACK"))
			.andExpect(jsonPath("$.data.aiUsage").isEmpty())
			.andExpect(content().string(not(containsString("authentication_error"))))
			.andExpect(content().string(not(containsString("Authentication Fails"))))
			.andExpect(content().string(not(containsString("api key"))));
	}

	private static class ProviderAuthenticationException extends RuntimeException {
		ProviderAuthenticationException(String message) {
			super(message);
		}
	}
}
