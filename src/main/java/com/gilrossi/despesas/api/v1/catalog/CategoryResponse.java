package com.gilrossi.despesas.api.v1.catalog;

import com.gilrossi.despesas.catalog.category.Category;

public record CategoryResponse(Long id, String name, boolean active) {

	public static CategoryResponse from(Category category) {
		return new CategoryResponse(category.getId(), category.getName(), category.isActive());
	}
}
