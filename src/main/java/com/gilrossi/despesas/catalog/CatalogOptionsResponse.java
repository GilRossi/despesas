package com.gilrossi.despesas.catalog;

import java.util.List;

public record CatalogOptionsResponse(Long id, String name, List<CatalogSubcategoryOption> subcategories) {
}
