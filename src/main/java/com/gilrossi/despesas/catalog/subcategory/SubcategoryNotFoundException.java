package com.gilrossi.despesas.catalog.subcategory;

public class SubcategoryNotFoundException extends RuntimeException {

	public SubcategoryNotFoundException(Long id) {
		super("Subcategory with id " + id + " was not found");
	}
}
