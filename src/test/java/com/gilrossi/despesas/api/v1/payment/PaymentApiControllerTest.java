package com.gilrossi.despesas.api.v1.payment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.payment.CreatePaymentRequest;
import com.gilrossi.despesas.payment.PaymentMethod;
import com.gilrossi.despesas.payment.PaymentResponse;
import com.gilrossi.despesas.payment.PaymentService;

@WebMvcTest(PaymentApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class PaymentApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PaymentService paymentService;

	@Test
	void deve_registrar_pagamento_e_retornar_status_da_despesa() throws Exception {
		when(paymentService.registrar(any(CreatePaymentRequest.class))).thenReturn(
			new PaymentResponse(
				10L,
				1L,
				new BigDecimal("40.00"),
				LocalDate.now(),
				PaymentMethod.PIX,
				"Pagamento parcial",
				ExpenseStatus.PARCIALMENTE_PAGA,
				new BigDecimal("40.00"),
				new BigDecimal("60.00"),
				null,
				null
			)
		);

		mockMvc.perform(post("/api/v1/payments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"expenseId": 1,
						"amount": 40.00,
						"paidAt": "%s",
						"method": "PIX",
						"notes": "Pagamento parcial"
					}
					""".formatted(LocalDate.now())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(10))
			.andExpect(jsonPath("$.data.expenseStatus").value("PARCIALMENTE_PAGA"));
	}

	@Test
	void deve_rejeitar_pagamento_com_payload_invalido() throws Exception {
		mockMvc.perform(post("/api/v1/payments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"expenseId": 1,
						"amount": -1,
						"paidAt": "%s",
						"method": "PIX"
					}
					""".formatted(LocalDate.now())))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.message").value("Request validation failed"));
	}
}
