package com.gilrossi.despesas.api.v1.shared;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
import com.gilrossi.despesas.emailingestion.EmailIngestionReviewActionNotAllowedException;
import com.gilrossi.despesas.emailingestion.EmailIngestionReviewNotFoundException;
import com.gilrossi.despesas.identity.AppUserNotFoundException;
import com.gilrossi.despesas.expense.ExpenseNotFoundException;
import com.gilrossi.despesas.fixedbill.FixedBillNotFoundException;
import com.gilrossi.despesas.identity.DuplicateRegistrationException;
import com.gilrossi.despesas.payment.PaymentBusinessRuleException;
import com.gilrossi.despesas.payment.PaymentNotFoundException;
import com.gilrossi.despesas.ratelimit.RateLimitExceededException;
import com.gilrossi.despesas.security.SecurityAuditLogger;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "com.gilrossi.despesas.api.v1")
public class ApiExceptionHandler {

	private final SecurityAuditLogger securityAuditLogger;

	public ApiExceptionHandler(ObjectProvider<SecurityAuditLogger> securityAuditLoggerProvider) {
		this.securityAuditLogger = securityAuditLoggerProvider.getIfAvailable();
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public org.springframework.http.ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception) {
		return ApiErrorResponses.validation(exception);
	}

	@ExceptionHandler(FieldBusinessRuleException.class)
	public ResponseEntity<ApiErrorResponse> fieldBusinessRule(FieldBusinessRuleException exception) {
		return ApiErrorResponses.unprocessable(exception.getMessage(), exception.getFieldErrors());
	}

	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
	public org.springframework.http.ResponseEntity<ApiErrorResponse> badRequest(Exception exception) {
		return ApiErrorResponses.badRequest("INVALID_REQUEST", "Request payload is invalid");
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorResponse> unauthorized(AuthenticationException exception) {
		return ApiErrorResponses.unauthorized("Authentication failed");
	}

	@ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
	public ResponseEntity<ApiErrorResponse> forbidden(Exception exception, HttpServletRequest request) {
		if (securityAuditLogger != null) {
			securityAuditLogger.accessDenied(request);
		}
		return ApiErrorResponses.forbidden("Access denied");
	}

	@ExceptionHandler(RateLimitExceededException.class)
	public ResponseEntity<ApiErrorResponse> tooManyRequests(RateLimitExceededException exception) {
		return ResponseEntity.status(429)
			.header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
			.body(ApiErrorResponses.body("RATE_LIMITED", "Too many requests"));
	}

	@ExceptionHandler({
		AppUserNotFoundException.class,
		CategoryNotFoundException.class,
		SubcategoryNotFoundException.class,
		ExpenseNotFoundException.class,
		FixedBillNotFoundException.class,
		com.gilrossi.despesas.payment.ExpenseNotFoundException.class,
		PaymentNotFoundException.class,
		EmailIngestionReviewNotFoundException.class
	})
	public ResponseEntity<ApiErrorResponse> notFound(RuntimeException exception) {
		return ApiErrorResponses.notFound(exception.getMessage());
	}

	@ExceptionHandler({
		DuplicateRegistrationException.class,
		DuplicateCategoryException.class,
		DuplicateSubcategoryException.class,
		DataIntegrityViolationException.class
	})
	public ResponseEntity<ApiErrorResponse> conflict(RuntimeException exception) {
		return ApiErrorResponses.conflict(exception.getMessage());
	}

	@ExceptionHandler({
		PaymentBusinessRuleException.class,
		EmailIngestionReviewActionNotAllowedException.class,
		IllegalArgumentException.class,
		IllegalStateException.class
	})
	public ResponseEntity<ApiErrorResponse> business(RuntimeException exception) {
		return ApiErrorResponses.unprocessable(exception.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> internalError(Exception exception) {
		return ApiErrorResponses.internalError();
	}
}
