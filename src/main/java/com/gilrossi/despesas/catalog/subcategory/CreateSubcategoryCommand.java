package com.gilrossi.despesas.catalog.subcategory;

public record CreateSubcategoryCommand(Long categoryId, String name, Boolean active) {
}
