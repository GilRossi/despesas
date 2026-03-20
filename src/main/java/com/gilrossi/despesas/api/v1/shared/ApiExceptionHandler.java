package com.gilrossi.despesas.api.v1.shared;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.gilrossi.despesas.catalog.category.CategoryNotFoundException;
import com.gilrossi.despesas.catalog.category.DuplicateCategoryException;
import com.gilrossi.despesas.catalog.subcategory.DuplicateSubcategoryException;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryNotFoundException;
import com.gilrossi.despesas.expense.ExpenseNotFoundException;
import com.gilrossi.despesas.identity.DuplicateRegistrationException;
import com.gilrossi.despesas.payment.PaymentBusinessRuleException;

@RestControllerAdvice(basePackages = "com.gilrossi.despesas.api.v1")
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public org.springframework.http.ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception) {
		return ApiErrorResponses.validation(exception);
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	public org.springframework.http.ResponseEntity<ApiErrorResponse> badRequest(Exception exception) {
		return ApiErrorResponses.badRequest("INVALID_REQUEST", "Request payload is invalid");
	}

	@ExceptionHandler(AuthenticationException.class)
	public org.springframework.http.ResponseEntity<ApiErrorResponse> unauthorized(AuthenticationException exception) {
		return ApiErrorResponses.unauthorized("Authentication failed");
	}

	@ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
	public org.springframework.http.ResponseEntity<ApiErrorResponse> forbidden(Exception exception) {
		return ApiErrorResponses.forbidden("Access denied");
	}

	@ExceptionHandler({
		CategoryNotFoundException.class,
		SubcategoryNotFoundException.class,
		ExpenseNotFoundException.class,
		com.gilrossi.despesas.payment.ExpenseNotFoundException.class
	})
	public org.springframework.http.ResponseEntity<ApiErrorResponse> notFound(RuntimeException exception) {
		return ApiErrorResponses.notFound(exception.getMessage());
	}

	@ExceptionHandler({
		DuplicateRegistrationException.class,
		DuplicateCategoryException.class,
		DuplicateSubcategoryException.class,
		DataIntegrityViolationException.class
	})
	public org.springframework.http.ResponseEntity<ApiErrorResponse> conflict(RuntimeException exception) {
		return ApiErrorResponses.conflict(exception.getMessage());
	}

	@ExceptionHandler({PaymentBusinessRuleException.class, IllegalArgumentException.class, IllegalStateException.class})
	public org.springframework.http.ResponseEntity<ApiErrorResponse> business(RuntimeException exception) {
		return ApiErrorResponses.unprocessable(exception.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public org.springframework.http.ResponseEntity<ApiErrorResponse> internalError(Exception exception) {
		return ApiErrorResponses.internalError();
	}
}
