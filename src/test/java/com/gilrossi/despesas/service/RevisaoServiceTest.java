package com.gilrossi.despesas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.emailingestion.EmailIngestionClassification;
import com.gilrossi.despesas.emailingestion.EmailIngestionDecisionReason;
import com.gilrossi.despesas.emailingestion.EmailIngestionFinalDecision;
import com.gilrossi.despesas.emailingestion.EmailIngestionRecord;
import com.gilrossi.despesas.emailingestion.EmailIngestionReviewActionResult;
import com.gilrossi.despesas.emailingestion.EmailIngestionReviewService;
import com.gilrossi.despesas.model.RevisaoPagina;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class RevisaoServiceTest {

	@Mock
	private EmailIngestionReviewService reviewService;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	private RevisaoService service;

	@BeforeEach
	void setUp() {
		service = new RevisaoService(reviewService, currentHouseholdProvider);
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(99L);
	}

	@Test
	void deve_mapear_pagina_de_revisao_com_rotulos_e_fallbacks() {
		when(reviewService.listPending(99L)).thenReturn(List.of(
			record(
				1L,
				EmailIngestionClassification.RECURRING_BILL,
				EmailIngestionDecisionReason.REVIEW_REQUESTED,
				"Academia",
				new BigDecimal("59.90"),
				"Resumo 1"
			),
			record(
				2L,
				EmailIngestionClassification.MANUAL_PURCHASE,
				EmailIngestionDecisionReason.CATEGORY_NOT_FOUND,
				"",
				null,
				""
			),
			record(
				3L,
				EmailIngestionClassification.FINANCIAL_TRANSACTION,
				EmailIngestionDecisionReason.SUBCATEGORY_NOT_FOUND,
				null,
				new BigDecimal("10.00"),
				null
			),
			record(
				4L,
				EmailIngestionClassification.IRRELEVANT,
				EmailIngestionDecisionReason.SUBCATEGORY_REQUIRED,
				"Posto",
				new BigDecimal("100.00"),
				"Resumo 4"
			),
			record(
				5L,
				EmailIngestionClassification.RECURRING_BILL,
				EmailIngestionDecisionReason.UNSUPPORTED_CURRENCY,
				"Exterior",
				new BigDecimal("200.00"),
				"Resumo 5"
			),
			record(
				6L,
				EmailIngestionClassification.MANUAL_PURCHASE,
				EmailIngestionDecisionReason.MISSING_TOTAL_AMOUNT,
				"Mercado",
				null,
				"Resumo 6"
			),
			record(
				7L,
				EmailIngestionClassification.FINANCIAL_TRANSACTION,
				EmailIngestionDecisionReason.ITEM_TOTAL_MISMATCH,
				"Loja",
				new BigDecimal("89.00"),
				"Resumo 7"
			),
			record(
				8L,
				EmailIngestionClassification.IRRELEVANT,
				EmailIngestionDecisionReason.LOW_CONFIDENCE,
				"Indefinido",
				new BigDecimal("18.00"),
				"Resumo 8"
			)
		));

		RevisaoPagina pagina = service.carregarPagina();

		assertEquals(8, pagina.totalPendencias());
		assertEquals(true, pagina.hasPendencias());
		assertEquals(0, new RevisaoPagina(null).totalPendencias());
		assertEquals(false, new RevisaoPagina(null).hasPendencias());

		assertEquals("Cobrança recorrente", pagina.pendencias().get(0).classificationLabel());
		assertEquals("Compra manual", pagina.pendencias().get(1).classificationLabel());
		assertEquals("Transação financeira", pagina.pendencias().get(2).classificationLabel());
		assertEquals("Irrelevante", pagina.pendencias().get(3).classificationLabel());

		assertEquals("Confiança insuficiente para autoimportação", pagina.pendencias().get(0).reasonLabel());
		assertEquals("Categoria sugerida não mapeada", pagina.pendencias().get(1).reasonLabel());
		assertEquals("Subcategoria sugerida não mapeada", pagina.pendencias().get(2).reasonLabel());
		assertEquals("Subcategoria não pôde ser resolvida", pagina.pendencias().get(3).reasonLabel());
		assertEquals("Moeda não suportada", pagina.pendencias().get(4).reasonLabel());
		assertEquals("Total não extraído", pagina.pendencias().get(5).reasonLabel());
		assertEquals("Itens não fecham com o total", pagina.pendencias().get(6).reasonLabel());
		assertEquals("Revisão manual necessária", pagina.pendencias().get(7).reasonLabel());

		assertEquals("Não identificado", pagina.pendencias().get(1).merchantOrPayee());
		assertEquals("Total ausente", pagina.pendencias().get(1).totalAmountLabel());
		assertEquals("Sem resumo extraído.", pagina.pendencias().get(1).summary());
		assertEquals("59,9%", pagina.pendencias().get(0).confidenceLabel());
	}

	@Test
	void deve_aprovar_e_rejeitar_com_mensagem_legivel() {
		when(reviewService.approve(99L, 15L)).thenReturn(new EmailIngestionReviewActionResult(
			15L,
			EmailIngestionFinalDecision.AUTO_IMPORTED,
			EmailIngestionDecisionReason.IMPORTED,
			70L
		));
		when(reviewService.reject(99L, 16L)).thenReturn(new EmailIngestionReviewActionResult(
			16L,
			EmailIngestionFinalDecision.IGNORED,
			EmailIngestionDecisionReason.MANUALLY_REJECTED,
			null
		));

		assertEquals(
			"Ingestão #15 aprovada e importada na despesa #70.",
			service.aprovar(15L)
		);
		assertEquals(
			"Ingestão #16 rejeitada e arquivada.",
			service.rejeitar(16L)
		);
	}

	private static EmailIngestionRecord record(
		Long id,
		EmailIngestionClassification classification,
		EmailIngestionDecisionReason decisionReason,
		String merchantOrPayee,
		BigDecimal totalAmount,
		String summary
	) {
		return new EmailIngestionRecord(
			id,
			99L,
			5L,
			"cartao@banco.com",
			"cartao@banco.com",
			"external-" + id,
			"sender@example.com",
			"Assunto " + id,
			OffsetDateTime.parse("2026-03-29T14:30:00Z"),
			merchantOrPayee,
			"Moradia",
			"Internet",
			totalAmount,
			null,
			null,
			"BRL",
			summary,
			classification,
			new BigDecimal("0.599"),
			"ref-" + id,
			null,
			EmailIngestionFinalDecision.REVIEW_REQUIRED,
			decisionReason,
			"fingerprint-" + id,
			null,
			OffsetDateTime.parse("2026-03-29T14:30:00Z"),
			OffsetDateTime.parse("2026-03-29T14:35:00Z"),
			List.of()
		);
	}
}
