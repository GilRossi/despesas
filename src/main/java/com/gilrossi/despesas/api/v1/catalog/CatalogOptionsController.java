package com.gilrossi.despesas.api.v1.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.catalog.CatalogOptionsResponse;
import com.gilrossi.despesas.catalog.CatalogQueryService;

@RestController
@RequestMapping("/api/v1/catalog/options")
public class CatalogOptionsController {

	private final CatalogQueryService catalogQueryService;

	public CatalogOptionsController(CatalogQueryService catalogQueryService) {
		this.catalogQueryService = catalogQueryService;
	}

	@GetMapping
	public ApiResponse<List<CatalogOptionsResponse>> listar() {
		return new ApiResponse<>(catalogQueryService.listarOpcoesAtivas());
	}
}
