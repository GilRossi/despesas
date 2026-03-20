package com.gilrossi.despesas.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
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
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.catalog.CatalogOptionsResponse;
import com.gilrossi.despesas.catalog.CatalogSubcategoryOption;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.model.Despesa;
import com.gilrossi.despesas.service.DespesaNotFoundException;
import com.gilrossi.despesas.service.DespesaService;

@WebMvcTest(DespesaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DespesaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DespesaService service;

	@Test
	void deve_listar_pagina_inicial_de_despesas_no_fluxo_novo() throws Exception {
		Despesa despesa = novaDespesa();
		despesa.setId(1L);
		when(service.listarDespesas(0, 10)).thenReturn(new PageImpl<>(List.of(despesa), PageRequest.of(0, 10), 1));

		mockMvc.perform(get("/despesas"))
			.andExpect(status().isOk())
			.andExpect(view().name("despesas/lista"))
			.andExpect(model().attributeExists("pagina"))
			.andExpect(model().attribute("despesas", hasSize(1)));
	}

	@Test
	void deve_carregar_formulario_novo_com_catalogo() throws Exception {
		when(service.listarCatalogo()).thenReturn(catalogo());

		mockMvc.perform(get("/despesas/nova"))
			.andExpect(status().isOk())
			.andExpect(view().name("despesas/form"))
			.andExpect(model().attributeExists("despesa"))
			.andExpect(model().attributeExists("catalogo"))
			.andExpect(model().attributeExists("contextos"));
	}

	@Test
	void deve_salvar_despesa_valida_e_redirecionar_com_sucesso() throws Exception {
		doNothing().when(service).salvar(any(Despesa.class));

		mockMvc.perform(post("/despesas/salvar")
				.param("descricao", "Internet da casa")
				.param("valor", "120.00")
				.param("data", "2026-03-20")
				.param("contexto", "CASA")
				.param("categoriaId", "10")
				.param("subcategoriaId", "20")
				.param("observacoes", "Conta fixa"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/despesas"))
			.andExpect(flash().attribute("mensagemSucesso", "Despesa cadastrada com sucesso."));

		verify(service).salvar(argThat(despesa ->
			despesa.getValor().compareTo(new BigDecimal("120.00")) == 0
				&& LocalDate.of(2026, 3, 20).equals(despesa.getData())
				&& "Internet da casa".equals(despesa.getDescricao())
				&& ExpenseContext.CASA == despesa.getContexto()
				&& Long.valueOf(10L).equals(despesa.getCategoriaId())
				&& Long.valueOf(20L).equals(despesa.getSubcategoriaId())
		));
	}

	@Test
	void deve_retornar_formulario_quando_validacao_falhar() throws Exception {
		when(service.listarCatalogo()).thenReturn(catalogo());

		mockMvc.perform(post("/despesas/salvar")
				.param("descricao", "")
				.param("valor", "0")
				.param("data", "")
				.param("contexto", "")
				.param("categoriaId", "")
				.param("subcategoriaId", ""))
			.andExpect(status().isOk())
			.andExpect(view().name("despesas/form"))
			.andExpect(model().attributeExists("catalogo"))
			.andExpect(model().attributeHasFieldErrors("despesa", "descricao", "valor", "data", "contexto", "categoriaId", "subcategoriaId"));
	}

	@Test
	void deve_carregar_formulario_de_edicao_no_fluxo_novo() throws Exception {
		Despesa despesa = novaDespesa();
		despesa.setId(7L);
		when(service.buscaPorId(7L)).thenReturn(despesa);
		when(service.listarCatalogo()).thenReturn(catalogo());

		mockMvc.perform(get("/despesas/editar/7"))
			.andExpect(status().isOk())
			.andExpect(view().name("despesas/form"))
			.andExpect(model().attributeExists("despesa"))
			.andExpect(model().attributeExists("catalogo"));
	}

	@Test
	void deve_excluir_despesa_por_post_no_fluxo_novo() throws Exception {
		doNothing().when(service).deletar(3L);

		mockMvc.perform(post("/despesas/3/excluir"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/despesas"))
			.andExpect(flash().attribute("mensagemSucesso", "Despesa excluída com sucesso."));

		verify(service).deletar(3L);
	}

	@Test
	void deve_redirecionar_com_erro_quando_despesa_nao_for_encontrada() throws Exception {
		when(service.buscaPorId(99L)).thenThrow(new DespesaNotFoundException(99L));

		mockMvc.perform(get("/despesas/editar/99"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/despesas"))
			.andExpect(flash().attribute("mensagemErro", "Despesa com ID 99 não foi encontrada."));
	}

	private Despesa novaDespesa() {
		Despesa despesa = new Despesa();
		despesa.setDescricao("Internet da casa");
		despesa.setValor(new BigDecimal("120.00"));
		despesa.setData(LocalDate.of(2026, 3, 20));
		despesa.setContexto(ExpenseContext.CASA);
		despesa.setCategoriaId(10L);
		despesa.setCategoria("Moradia");
		despesa.setSubcategoriaId(20L);
		despesa.setSubcategoria("Internet");
		despesa.setObservacoes("Conta fixa");
		return despesa;
	}

	private List<CatalogOptionsResponse> catalogo() {
		return List.of(
			new CatalogOptionsResponse(10L, "Moradia", List.of(new CatalogSubcategoryOption(20L, "Internet")))
		);
	}
}
