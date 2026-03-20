package com.gilrossi.despesas.api.v1.catalog;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.catalog.CatalogOptionsResponse;
import com.gilrossi.despesas.catalog.CatalogQueryService;
import com.gilrossi.despesas.catalog.CatalogSubcategoryOption;

@WebMvcTest(CatalogOptionsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class CatalogOptionsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CatalogQueryService catalogQueryService;

	@Test
	void deve_retornar_opcoes_de_catalogo_para_flutter() throws Exception {
		when(catalogQueryService.listarOpcoesAtivas()).thenReturn(List.of(
			new CatalogOptionsResponse(10L, "Casa", List.of(
				new CatalogSubcategoryOption(100L, "Internet"),
				new CatalogSubcategoryOption(101L, "Mercado")
			))
		));

		mockMvc.perform(get("/api/v1/catalog/options"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].id").value(10))
			.andExpect(jsonPath("$.data[0].subcategories[1].name").value("Mercado"));
	}
}
