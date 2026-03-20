package com.gilrossi.despesas.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.financialassistant.MonthComparisonResponse;
import com.gilrossi.despesas.financialassistant.RecommendationResponse;
import com.gilrossi.despesas.model.RelatorioAssistenteAcao;
import com.gilrossi.despesas.model.RelatorioAssistenteResposta;
import com.gilrossi.despesas.model.RelatorioPagina;
import com.gilrossi.despesas.service.RelatorioService;

@WebMvcTest(RelatorioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RelatorioControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RelatorioService service;

	@Test
	void deve_renderizar_pagina_de_relatorios() throws Exception {
		when(service.carregarPagina("2026-03", true)).thenReturn(relatorioBase(true));

		mockMvc.perform(get("/relatorios").param("referenceMonth", "2026-03").param("comparePrevious", "true"))
			.andExpect(status().isOk())
			.andExpect(view().name("despesas/relatorios"))
			.andExpect(model().attributeExists("relatorio"));
	}

	@Test
	void deve_redirecionar_quando_periodo_for_invalido() throws Exception {
		when(service.carregarPagina("2026-13", true)).thenThrow(new IllegalArgumentException("Período inválido. Use o formato yyyy-MM."));

		mockMvc.perform(get("/relatorios").param("referenceMonth", "2026-13"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/relatorios"))
			.andExpect(flash().attribute("mensagemErro", "Período inválido. Use o formato yyyy-MM."));
	}

	@Test
	void deve_consultar_atalho_do_assistente_e_redirecionar_para_mes_atual() throws Exception {
		when(service.executarAtalho("COMO_ECONOMIZAR", "2026-03"))
			.thenReturn(new RelatorioAssistenteResposta(
				"Como economizar?",
				"Como posso economizar este mês?",
				"Revise as despesas variáveis.",
				com.gilrossi.despesas.financialassistant.FinancialAssistantQueryMode.AI,
				com.gilrossi.despesas.financialassistant.FinancialAssistantIntent.SAVINGS_RECOMMENDATIONS
			));

		mockMvc.perform(post("/relatorios/assistente")
				.param("action", "COMO_ECONOMIZAR")
				.param("referenceMonth", "2026-03")
				.param("comparePrevious", "false"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/relatorios?referenceMonth=2026-03&comparePrevious=false"))
			.andExpect(flash().attributeExists("assistantResponse"));

		verify(service).executarAtalho("COMO_ECONOMIZAR", "2026-03");
	}

	@Test
	void deve_redirecionar_com_erro_quando_atalho_for_invalido() throws Exception {
		when(service.executarAtalho("INVALIDO", "2026-03")).thenThrow(new IllegalArgumentException("Atalho do assistente inválido."));

		mockMvc.perform(post("/relatorios/assistente")
				.param("action", "INVALIDO")
				.param("referenceMonth", "2026-03"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/relatorios?referenceMonth=2026-03&comparePrevious=true"))
			.andExpect(flash().attribute("mensagemErro", "Atalho do assistente inválido."));
	}

	private RelatorioPagina relatorioBase(boolean compareWithPrevious) {
		return new RelatorioPagina(
			YearMonth.of(2026, 3),
			"2026-03",
			"Março de 2026",
			compareWithPrevious,
			4,
			new BigDecimal("3360.00"),
			new BigDecimal("1520.00"),
			new BigDecimal("1840.00"),
			"Moradia",
			new BigDecimal("1320.00"),
			new BigDecimal("39.29"),
			new MonthComparisonResponse("2026-03", new BigDecimal("3360.00"), "2026-02", new BigDecimal("2180.00"), new BigDecimal("1180.00"), new BigDecimal("54.13")),
			100,
			65,
			45,
			55,
			List.of(),
			List.of(),
			List.of(),
			List.of(),
			List.of(new RecommendationResponse("Revise", "Resumo", "Ação")),
			List.of(RelatorioAssistenteAcao.values())
		);
	}
}
