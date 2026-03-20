package com.gilrossi.despesas.catalog.subcategory;

public record UpdateSubcategoryCommand(Long categoryId, String name, Boolean active) {
}
