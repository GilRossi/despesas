package com.gilrossi.despesas.api.v1.shared;

import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

public final class ApiErrorResponses {

	private ApiErrorResponses() {
	}

	public static ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception) {
		LinkedHashMap<String, String> uniqueErrors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			uniqueErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}
		List<FieldErrorResponse> fieldErrors = uniqueErrors.entrySet().stream()
			.map(entry -> new FieldErrorResponse(entry.getKey(), entry.getValue()))
			.toList();
		return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", fieldErrors);
	}

	public static ResponseEntity<ApiErrorResponse> badRequest(String code, String message) {
		return build(HttpStatus.BAD_REQUEST, code, message, List.of());
	}

	public static ResponseEntity<ApiErrorResponse> unauthorized(String message) {
		return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message, List.of());
	}

	public static ResponseEntity<ApiErrorResponse> forbidden(String message) {
		return build(HttpStatus.FORBIDDEN, "FORBIDDEN", message, List.of());
	}

	public static ResponseEntity<ApiErrorResponse> notFound(String message) {
		return build(HttpStatus.NOT_FOUND, "NOT_FOUND", message, List.of());
	}

	public static ResponseEntity<ApiErrorResponse> conflict(String message) {
		return build(HttpStatus.CONFLICT, "CONFLICT", message, List.of());
	}

	public static ResponseEntity<ApiErrorResponse> unprocessable(String message) {
		return build(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE", message, List.of());
	}

	public static ResponseEntity<ApiErrorResponse> internalError() {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", List.of());
	}

	public static ApiErrorResponse body(String code, String message) {
		return new ApiErrorResponse(code, message, List.of());
	}

	private static ResponseEntity<ApiErrorResponse> build(
		HttpStatus status,
		String code,
		String message,
		List<FieldErrorResponse> fieldErrors
	) {
		return ResponseEntity.status(status).body(new ApiErrorResponse(code, message, fieldErrors));
	}
}
