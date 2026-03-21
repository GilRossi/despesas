package com.gilrossi.despesas.api.v1.emailingestion;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.api.v1.shared.PageInfo;
import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.emailingestion.EmailIngestionClassification;
import com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason;
import com.gilrossi.despesas.emailingestion.EmailIngestionDesiredDecision;
import com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision;

@WebMvcTest(EmailIngestionReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class EmailIngestionReviewControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmailIngestionReviewApiService service;

	@Test
	void deve_listar_pendencias_com_paginacao() throws Exception {
		when(service.list(EmailIngestionReviewStatus.PENDING, 0, 20)).thenReturn(new PageResponse<>(
			List.of(new EmailIngestionReviewSummaryResponse(
				51L,
				"financeiro@gmail.com",
				"noreply@cobasi.com.br",
				"Compra Cobasi",
				OffsetDateTime.parse("2026-03-19T10:15:30Z"),
				"Cobasi",
				new BigDecimal("289.70"),
				"BRL",
				"Compra pet shop",
				EmailIngestionClassification.MANUAL_PURCHASE,
				new BigDecimal("0.72"),
				EmailIngestionDecisionReason.REVIEW_REQUESTED
			)),
			new PageInfo(0, 20, 1, 1, false, false)
		));

		mockMvc.perform(get("/api/v1/email-ingestion/reviews")
				.param("status", "PENDING")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].ingestionId").value(51))
			.andExpect(jsonPath("$.content[0].classification").value("MANUAL_PURCHASE"))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void deve_detalhar_pendencia() throws Exception {
		when(service.detail(51L)).thenReturn(new EmailIngestionReviewDetailResponse(
			51L,
			"financeiro@gmail.com",
			"msg-1",
			"noreply@cobasi.com.br",
			"Compra Cobasi",
			OffsetDateTime.parse("2026-03-19T10:15:30Z"),
			"Cobasi",
			"Pets",
			"Pet shop",
			new BigDecimal("289.70"),
			LocalDate.parse("2026-03-25"),
			LocalDate.parse("2026-03-19"),
			"BRL",
			"Compra pet shop",
			EmailIngestionClassification.MANUAL_PURCHASE,
			new BigDecimal("0.72"),
			"gmail:msg-1",
			EmailIngestionDesiredDecision.REVIEW,
			EmailIngestionFinalDecision.REVIEW_REQUIRED,
			EmailIngestionDecisionReason.REVIEW_REQUESTED,
			null,
			OffsetDateTime.parse("2026-03-19T10:15:30Z"),
			OffsetDateTime.parse("2026-03-19T10:16:30Z"),
			List.of(new EmailIngestionReviewItemResponse("Ração", new BigDecimal("289.70"), null))
		));

		mockMvc.perform(get("/api/v1/email-ingestion/reviews/51"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ingestionId").value(51))
			.andExpect(jsonPath("$.data.items[0].description").value("Ração"))
			.andExpect(jsonPath("$.data.decisionReason").value("REVIEW_REQUESTED"));
	}

	@Test
	void deve_aprovar_pendencia() throws Exception {
		when(service.approve(51L)).thenReturn(new EmailIngestionReviewActionResponse(
			51L,
			EmailIngestionFinalDecision.AUTO_IMPORTED,
			EmailIngestionDecisionReason.MANUALLY_IMPORTED,
			88L
		));

		mockMvc.perform(post("/api/v1/email-ingestion/reviews/51/approve"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ingestionId").value(51))
			.andExpect(jsonPath("$.data.decision").value("AUTO_IMPORTED"))
			.andExpect(jsonPath("$.data.expenseId").value(88));
	}

	@Test
	void deve_rejeitar_pendencia() throws Exception {
		when(service.reject(51L)).thenReturn(new EmailIngestionReviewActionResponse(
			51L,
			EmailIngestionFinalDecision.IGNORED,
			EmailIngestionDecisionReason.MANUALLY_REJECTED,
			null
		));

		mockMvc.perform(post("/api/v1/email-ingestion/reviews/51/reject"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.ingestionId").value(51))
			.andExpect(jsonPath("$.data.decision").value("IGNORED"))
			.andExpect(jsonPath("$.data.expenseId").doesNotExist());
	}
}
