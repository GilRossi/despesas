package com.gilrossi.despesas.api.v1.catalog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryService;

@WebMvcTest(SubcategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class SubcategoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SubcategoryService service;

	@Test
	void deve_listar_subcategorias_em_json() throws Exception {
		PageImpl<Subcategory> pagina = new PageImpl<>(List.of(new Subcategory(1L, 10L, "Mercado", true)), PageRequest.of(0, 10), 1);
		when(service.listar(10L, "mer", true, 0, 10)).thenReturn(pagina);

		mockMvc.perform(get("/api/v1/subcategories")
				.param("categoryId", "10")
				.param("q", "mer")
				.param("active", "true"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].name").value("Mercado"))
			.andExpect(jsonPath("$.content[0].categoryId").value(10));
	}

	@Test
	void deve_criar_subcategoria() throws Exception {
		when(service.criar(any())).thenReturn(new Subcategory(1L, 10L, "Mercado", true));

		mockMvc.perform(post("/api/v1/subcategories")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"categoryId":10,"name":"Mercado","active":true}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.categoryId").value(10));

		verify(service).criar(any());
	}

	@Test
	void deve_validar_subcategoria_sem_categoria() throws Exception {
		mockMvc.perform(post("/api/v1/subcategories")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"Mercado","active":true}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Request validation failed"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("categoryId"));
	}

	@Test
	void deve_atualizar_subcategoria() throws Exception {
		when(service.atualizar(eq(1L), any())).thenReturn(new Subcategory(1L, 10L, "Mercado", false));

		mockMvc.perform(put("/api/v1/subcategories/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"categoryId":10,"name":"Mercado","active":false}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.active").value(false));
	}

	@Test
	void deve_desativar_subcategoria() throws Exception {
		mockMvc.perform(delete("/api/v1/subcategories/9"))
			.andExpect(status().isNoContent());

		verify(service).desativar(9L);
	}
}
