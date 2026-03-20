package com.gilrossi.despesas.api.v1.catalog;

import com.gilrossi.despesas.catalog.subcategory.Subcategory;

public record SubcategoryResponse(Long id, Long categoryId, String name, boolean active) {

	public static SubcategoryResponse from(Subcategory subcategory) {
		return new SubcategoryResponse(
			subcategory.getId(),
			subcategory.getCategoryId(),
			subcategory.getName(),
			subcategory.isActive()
		);
	}
}
