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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.emailingestion.EmailIngestionReviewActionNotAllowedException;
import com.gilrossi.despesas.model.RevisaoPagina;
import com.gilrossi.despesas.model.RevisaoPendencia;
import com.gilrossi.despesas.service.RevisaoService;

@WebMvcTest(RevisaoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RevisaoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RevisaoService service;

	@Test
	void deve_renderizar_pagina_de_revisoes() throws Exception {
		when(service.carregarPagina()).thenReturn(new RevisaoPagina(List.of(
			new RevisaoPendencia(
				51L,
				"financeiro@gmail.com",
				"noreply@cobasi.com.br",
				"Compra Cobasi",
				"19/03/2026 07:15",
				"Compra manual",
				"72%",
				"Cobasi",
				"R$ 289,70",
				"Confiança insuficiente para autoimportação",
				"Compra pet shop"
			)
		)));

		mockMvc.perform(get("/revisoes"))
			.andExpect(status().isOk())
			.andExpect(view().name("despesas/revisoes"))
			.andExpect(model().attributeExists("revisaoPagina"));
	}

	@Test
	void deve_aprovar_pendencia_e_redirecionar_com_sucesso() throws Exception {
		when(service.aprovar(51L)).thenReturn("Ingestão #51 aprovada e importada na despesa #88.");

		mockMvc.perform(post("/revisoes/51/aprovar"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/revisoes"))
			.andExpect(flash().attribute("mensagemSucesso", "Ingestão #51 aprovada e importada na despesa #88."));

		verify(service).aprovar(51L);
	}

	@Test
	void deve_rejeitar_pendencia_invalida_e_redirecionar_com_erro() throws Exception {
		when(service.rejeitar(99L)).thenThrow(new EmailIngestionReviewActionNotAllowedException("A ingestão selecionada não está mais pendente de revisão."));

		mockMvc.perform(post("/revisoes/99/rejeitar"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/revisoes"))
			.andExpect(flash().attribute("mensagemErro", "A ingestão selecionada não está mais pendente de revisão."));
	}
}
