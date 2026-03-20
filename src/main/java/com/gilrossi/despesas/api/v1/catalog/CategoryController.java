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
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryService;
import com.gilrossi.despesas.catalog.category.CreateCategoryCommand;
import com.gilrossi.despesas.catalog.category.UpdateCategoryCommand;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

	private final CategoryService service;

	public CategoryController(CategoryService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<CategoryResponse> listar(
		@RequestParam(required = false) String q,
		@RequestParam(required = false) Boolean active,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		Page<Category> categorias = service.listar(q, active, page, size);
		return new PageResponse<>(
			categorias.map(CategoryResponse::from).getContent(),
			new PageInfo(
				categorias.getNumber(),
				categorias.getSize(),
				categorias.getTotalElements(),
				categorias.getTotalPages(),
				categorias.hasNext(),
				categorias.hasPrevious()
			)
		);
	}

	@GetMapping("/{id}")
	public ApiResponse<CategoryResponse> buscarPorId(@PathVariable Long id) {
		return new ApiResponse<>(CategoryResponse.from(service.buscarPorId(id)));
	}

	@PostMapping
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<ApiResponse<CategoryResponse>> criar(@Valid @RequestBody CategoryRequest request) {
		Category categoria = service.criar(new CreateCategoryCommand(request.name(), request.active()));
		return ResponseEntity.status(201).body(new ApiResponse<>(CategoryResponse.from(categoria)));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	public ApiResponse<CategoryResponse> atualizar(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		Category categoria = service.atualizar(id, new UpdateCategoryCommand(request.name(), request.active()));
		return new ApiResponse<>(CategoryResponse.from(categoria));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('OWNER')")
	public ResponseEntity<Void> desativar(@PathVariable Long id) {
		service.desativar(id);
		return ResponseEntity.noContent().build();
	}
}
