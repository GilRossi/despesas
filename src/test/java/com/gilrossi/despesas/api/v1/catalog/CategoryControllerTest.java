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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryService;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class CategoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CategoryService service;

	@Test
	void deve_listar_categorias_em_json() throws Exception {
		Page<Category> pagina = new PageImpl<>(List.of(new Category(1L, "Casa", true)), PageRequest.of(0, 10), 1);
		when(service.listar("ca", true, 0, 10)).thenReturn(pagina);

		mockMvc.perform(get("/api/v1/categories")
				.param("q", "ca")
				.param("active", "true"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].name").value("Casa"))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void deve_criar_categoria_e_retornar_201() throws Exception {
		when(service.criar(any())).thenReturn(new Category(1L, "Casa", true));

		mockMvc.perform(post("/api/v1/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"Casa","active":true}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.id").value(1))
			.andExpect(jsonPath("$.data.name").value("Casa"));

		verify(service).criar(any());
	}

	@Test
	void deve_validar_categoria_com_nome_vazio() throws Exception {
		mockMvc.perform(post("/api/v1/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":" ","active":true}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Request validation failed"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
	}

	@Test
	void deve_atualizar_categoria() throws Exception {
		when(service.atualizar(eq(1L), any())).thenReturn(new Category(1L, "Carro", false));

		mockMvc.perform(put("/api/v1/categories/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"name":"Carro","active":false}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.name").value("Carro"))
			.andExpect(jsonPath("$.data.active").value(false));
	}

	@Test
	void deve_desativar_categoria() throws Exception {
		mockMvc.perform(delete("/api/v1/categories/9"))
			.andExpect(status().isNoContent());

		verify(service).desativar(9L);
	}
}
