package com.gilrossi.despesas.api.v1.shared;

import java.util.List;

public record PageResponse<T>(List<T> content, PageInfo page) {
}
