package com.gilrossi.despesas.api.v1.income;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.api.v1.shared.FieldBusinessRuleException;
import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;
import com.gilrossi.despesas.income.CreateIncomeRequest;
import com.gilrossi.despesas.income.IncomeResponse;
import com.gilrossi.despesas.income.IncomeService;

import static org.mockito.Mockito.when;

@WebMvcTest(IncomeApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class IncomeApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IncomeService incomeService;

	@Test
	void deve_criar_ganho_e_retornar_resposta_minima() throws Exception {
		when(incomeService.create(any(CreateIncomeRequest.class))).thenReturn(new IncomeResponse(
			10L,
			"Salário principal",
			new BigDecimal("4500.00"),
			LocalDate.of(2026, 3, 28),
			new ReferenceResponse(7L, "Projeto Acme"),
			Instant.parse("2026-03-28T12:00:00Z")
		));

		mockMvc.perform(post("/api/v1/incomes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "description":"Salário principal",
					  "amount":4500.00,
					  "receivedOn":"2026-03-28",
					  "spaceReferenceId":7
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(10))
			.andExpect(jsonPath("$.data.description").value("Salário principal"))
			.andExpect(jsonPath("$.data.amount").value(4500.00))
			.andExpect(jsonPath("$.data.receivedOn").value("2026-03-28"))
			.andExpect(jsonPath("$.data.spaceReference.id").value(7))
			.andExpect(jsonPath("$.data.spaceReference.name").value("Projeto Acme"))
			.andExpect(jsonPath("$.data.createdAt").value("2026-03-28T12:00:00Z"));
	}

	@Test
	void deve_rejeitar_payload_invalido_quando_descricao_estiver_vazia() throws Exception {
		mockMvc.perform(post("/api/v1/incomes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "description":"",
					  "amount":4500.00,
					  "receivedOn":"2026-03-28"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("Request validation failed"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("description"));
	}

	@Test
	void deve_retornar_field_error_quando_referencia_nao_pertencer_ao_household() throws Exception {
		when(incomeService.create(any(CreateIncomeRequest.class))).thenThrow(
			new FieldBusinessRuleException(
				"spaceReferenceId",
				"spaceReferenceId must belong to the active household"
			)
		);

		mockMvc.perform(post("/api/v1/incomes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "description":"Freelance",
					  "amount":800.00,
					  "receivedOn":"2026-03-28",
					  "spaceReferenceId":99
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("BUSINESS_RULE"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("spaceReferenceId"));
	}
}
