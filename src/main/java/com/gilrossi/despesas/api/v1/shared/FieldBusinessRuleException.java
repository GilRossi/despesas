package com.gilrossi.despesas.api.v1.shared;

import java.util.List;

public class FieldBusinessRuleException extends RuntimeException {

	private final List<FieldErrorResponse> fieldErrors;

	public FieldBusinessRuleException(String field, String message) {
		this(message, List.of(new FieldErrorResponse(field, message)));
	}

	public FieldBusinessRuleException(String message, List<FieldErrorResponse> fieldErrors) {
		super(message);
		this.fieldErrors = List.copyOf(fieldErrors);
	}

	public List<FieldErrorResponse> getFieldErrors() {
		return fieldErrors;
	}
}
