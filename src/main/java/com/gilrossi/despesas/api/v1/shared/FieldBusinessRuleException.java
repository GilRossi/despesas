package com.gilrossi.despesas.api.v1.shared;

import java.util.List;

public class FieldBusinessRuleException extends RuntimeException {

	private final List<FieldErrorResponse> fieldErrors;

	public FieldBusinessRuleException(String field, String message) {
		super(message);
		this.fieldErrors = List.of(new FieldErrorResponse(field, message));
	}

	public List<FieldErrorResponse> getFieldErrors() {
		return fieldErrors;
	}
}
