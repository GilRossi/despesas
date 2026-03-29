package com.gilrossi.despesas.financialassistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class FinancialAssistantSupportTest {

	@Test
	void deve_resolver_intervalos_com_defaults_e_validacao() {
		FinancialAssistantDateRange fullMonth = FinancialAssistantSupport.resolveRange(null, null);
		assertEquals(YearMonth.now().atDay(1), fullMonth.from());
		assertEquals(YearMonth.now().atEndOfMonth(), fullMonth.to());

		FinancialAssistantDateRange fromOnly = FinancialAssistantSupport.resolveRange(LocalDate.of(2026, 3, 15), null);
		assertEquals(LocalDate.of(2026, 3, 15), fromOnly.from());
		assertEquals(LocalDate.of(2026, 3, 31), fromOnly.to());

		FinancialAssistantDateRange toOnly = FinancialAssistantSupport.resolveRange(null, LocalDate.of(2026, 2, 10));
		assertEquals(LocalDate.of(2026, 2, 1), toOnly.from());
		assertEquals(LocalDate.of(2026, 2, 10), toOnly.to());

		assertThrows(
			IllegalArgumentException.class,
			() -> FinancialAssistantSupport.resolveRange(
				LocalDate.of(2026, 3, 20),
				LocalDate.of(2026, 3, 10)
			)
		);
	}

	@Test
	void deve_resolver_referencia_de_mes_normalizar_texto_e_rotular_mes() {
		assertEquals(YearMonth.now(), FinancialAssistantSupport.resolveReferenceMonth(null));
		assertEquals(YearMonth.now(), FinancialAssistantSupport.resolveReferenceMonth("   "));
		assertEquals(
			YearMonth.of(2026, 3),
			FinancialAssistantSupport.resolveReferenceMonth(" 2026-03 ")
		);
		assertEquals("", FinancialAssistantSupport.normalizeText(null));
		assertEquals(
			"cafe com leite",
			FinancialAssistantSupport.normalizeText("  Café   com   leite ")
		);
		assertEquals("2026-04", FinancialAssistantSupport.monthLabel(YearMonth.of(2026, 4)));

		assertThrows(
			IllegalArgumentException.class,
			() -> FinancialAssistantSupport.resolveReferenceMonth("03/2026")
		);
	}

	@Test
	void deve_calcular_percentuais_e_variacao_percentual() {
		assertEquals(
			new BigDecimal("25.00"),
			FinancialAssistantSupport.percentage(
				new BigDecimal("50"),
				new BigDecimal("200")
			)
		);
		assertEquals(
			new BigDecimal("0.00"),
			FinancialAssistantSupport.percentage(null, new BigDecimal("200"))
		);
		assertEquals(
			new BigDecimal("0.00"),
			FinancialAssistantSupport.percentage(new BigDecimal("50"), BigDecimal.ZERO)
		);

		assertEquals(
			new BigDecimal("50.00"),
			FinancialAssistantSupport.percentageChange(
				new BigDecimal("150"),
				new BigDecimal("100")
			)
		);
		assertEquals(
			new BigDecimal("100.00"),
			FinancialAssistantSupport.percentageChange(
				new BigDecimal("10"),
				BigDecimal.ZERO
			)
		);
		assertEquals(
			new BigDecimal("0.00"),
			FinancialAssistantSupport.percentageChange(null, null)
		);
	}
}
