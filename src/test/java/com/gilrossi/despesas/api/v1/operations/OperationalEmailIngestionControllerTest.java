package com.gilrossi.despesas.api.v1.operations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason;
import com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision;
import com.gilrossi.despesas.emailingestion.EmailIngestionResult;
import com.gilrossi.despesas.emailingestion.EmailIngestionService;

@WebMvcTest(OperationalEmailIngestionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class OperationalEmailIngestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmailIngestionService service;

	@Test
	void deve_processar_payload_operacional_valido() throws Exception {
		when(service.process(any())).thenReturn(new EmailIngestionResult(
			10L,
			20L,
			EmailIngestionFinalDecision.AUTO_IMPORTED,
			EmailIngestionDecisionReason.IMPORTED,
			30L,
			false,
			"Candidate was imported into expenses"
		));

		mockMvc.perform(post("/api/v1/operations/email-ingestions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"financeiro@gmail.com",
					  "externalMessageId":"msg-1",
					  "sender":"noreply@cobasi.com.br",
					  "subject":"Compra Cobasi",
					  "receivedAt":"2026-03-19T10:15:30Z",
					  "merchantOrPayee":"Cobasi",
					  "suggestedCategoryName":"Pets",
					  "suggestedSubcategoryName":"Pet shop",
					  "totalAmount":289.70,
					  "occurredOn":"2026-03-19",
					  "currency":"BRL",
					  "items":[{"description":"Ração","amount":289.70}],
					  "summary":"Compra pet shop",
					  "classification":"MANUAL_PURCHASE",
					  "confidence":0.97,
					  "rawReference":"gmail:msg-1",
					  "desiredDecision":"AUTO_IMPORT"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ingestionId").value(10))
			.andExpect(jsonPath("$.data.decision").value("AUTO_IMPORTED"))
			.andExpect(jsonPath("$.data.expenseId").value(30));
	}

	@Test
	void deve_validar_payload_operacional_invalido() throws Exception {
		mockMvc.perform(post("/api/v1/operations/email-ingestions")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "sourceAccount":"financeiro@gmail.com",
					  "externalMessageId":"msg-1",
					  "sender":"noreply@cobasi.com.br",
					  "subject":"Compra Cobasi",
					  "receivedAt":"2026-03-19T10:15:30Z",
					  "currency":"BRL",
					  "classification":"MANUAL_PURCHASE",
					  "confidence":1.20,
					  "rawReference":"gmail:msg-1",
					  "desiredDecision":"AUTO_IMPORT"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}
}
