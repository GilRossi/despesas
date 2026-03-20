package com.gilrossi.despesas.api.v1.shared;

public record PageInfo(int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {
}
