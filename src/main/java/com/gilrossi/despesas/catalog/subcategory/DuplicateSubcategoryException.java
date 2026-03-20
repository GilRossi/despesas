package com.gilrossi.despesas.catalog.subcategory;

public class DuplicateSubcategoryException extends RuntimeException {

	public DuplicateSubcategoryException(String name) {
		super("Subcategory with name '" + name + "' already exists");
	}
}
