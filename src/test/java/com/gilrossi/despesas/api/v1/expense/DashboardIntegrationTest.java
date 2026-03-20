package com.gilrossi.despesas.api.v1.expense;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.gilrossi.despesas.payment.Payment;
import com.gilrossi.despesas.payment.PaymentMethod;
import com.gilrossi.despesas.payment.PaymentRepository;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegistrationService registrationService;

	@Autowired
	private JpaCategoryRepositoryAdapter categoryRepository;

	@Autowired
	private JpaSubcategoryRepositoryAdapter subcategoryRepository;

	@Autowired
	private ExpenseRepository expenseRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void deve_retornar_resumo_financeiro_do_household_autenticado() throws Exception {
		RegistrationResponse ana = registrationService.register(new RegistrationRequest("Ana", "ana-dashboard@local.invalid", "senha123", "Casa Ana Dashboard"));
		Category moradia = categoryRepository.save(ana.householdId(), new Category(null, "Moradia", true));
		Subcategory internet = subcategoryRepository.save(ana.householdId(), new Subcategory(null, moradia.getId(), "Internet", true));
		Subcategory mercado = subcategoryRepository.save(ana.householdId(), new Subcategory(null, moradia.getId(), "Mercado", true));

		Expense vencida = expenseRepository.save(new Expense(
			ana.householdId(),
			"Internet",
			new BigDecimal("120.00"),
			LocalDate.now().minusDays(1),
			ExpenseContext.CASA,
			moradia.getId(),
			moradia.getName(),
			internet.getId(),
			internet.getName(),
			null
		));
		Expense prevista = expenseRepository.save(new Expense(
			ana.householdId(),
			"Mercado",
			new BigDecimal("200.00"),
			LocalDate.now().plusDays(4),
			ExpenseContext.CASA,
			moradia.getId(),
			moradia.getName(),
			mercado.getId(),
			mercado.getName(),
			null
		));

		paymentRepository.save(new Payment(
			prevista.getId(),
			new BigDecimal("50.00"),
			LocalDate.now(),
			PaymentMethod.PIX,
			"Entrada"
		));
		String accessToken = loginApi("ana-dashboard@local.invalid", "senha123");

		mockMvc.perform(get("/api/v1/dashboard/summary")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.householdId").value(ana.householdId()))
			.andExpect(jsonPath("$.data.totalExpenses").value(2))
			.andExpect(jsonPath("$.data.totalAmount").value(320.00))
			.andExpect(jsonPath("$.data.paidAmount").value(50.00))
			.andExpect(jsonPath("$.data.remainingAmount").value(270.00))
			.andExpect(jsonPath("$.data.overdueCount").value(1));
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
