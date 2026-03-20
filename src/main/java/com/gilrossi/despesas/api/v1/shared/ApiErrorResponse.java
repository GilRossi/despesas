package com.gilrossi.despesas.api.v1.shared;

import java.util.List;

public record ApiErrorResponse(String code, String message, List<FieldErrorResponse> fieldErrors) {
}
