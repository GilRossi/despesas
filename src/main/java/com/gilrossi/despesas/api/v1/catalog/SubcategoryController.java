package com.gilrossi.despesas.api.v1.catalog;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.api.v1.shared.PageInfo;
import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.catalog.subcategory.CreateSubcategoryCommand;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryService;
import com.gilrossi.despesas.catalog.subcategory.UpdateSubcategoryCommand;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/subcategories")
public class SubcategoryController {

	private final SubcategoryService service;

	public SubcategoryController(SubcategoryService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<SubcategoryResponse> listar(
		@RequestParam(required = false) Long categoryId,
		@RequestParam(required = false) String q,
		@RequestParam(required = false) Boolean active,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		Page<Subcategory> subcategories = service.listar(categoryId, q, active, page, size);
		return new PageResponse<>(
			subcategories.map(SubcategoryResponse::from).getContent(),
			new PageInfo(
				subcategories.getNumber(),
				subcategories.getSize(),
				subcategories.getTotalElements(),
				subcategories.getTotalPages(),
				subcategories.hasNext(),
				subcategories.hasPrevious()
			)
		);
	}

	@GetMapping("/{id}")
	public ApiResponse<SubcategoryResponse> buscarPorId(@PathVariable Long id) {
		return new ApiResponse<>(SubcategoryResponse.from(service.buscarPorId(id)));
	}

	@PostMapping
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<SubcategoryResponse>> criar(@Valid @RequestBody SubcategoryRequest request) {
		Subcategory subcategory = service.criar(new CreateSubcategoryCommand(request.categoryId(), request.name(), request.active()));
		return ResponseEntity.status(201).body(new ApiResponse<>(SubcategoryResponse.from(subcategory)));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	public ApiResponse<SubcategoryResponse> atualizar(@PathVariable Long id, @Valid @RequestBody SubcategoryRequest request) {
		Subcategory subcategory = service.atualizar(id, new UpdateSubcategoryCommand(request.categoryId(), request.name(), request.active()));
		return new ApiResponse<>(SubcategoryResponse.from(subcategory));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<Void> desativar(@PathVariable Long id) {
		service.desativar(id);
		return ResponseEntity.noContent().build();
	}
}
