package com.gilrossi.despesas.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.api.v1.shared.ApiErrorResponse;
import com.gilrossi.despesas.api.v1.shared.ApiErrorResponses;

public class OperationalSignedRequestAuthenticationFilter extends OncePerRequestFilter {

	private final OperationalEmailIngestionProperties properties;
	private final OperationalRequestSignatureVerifier signatureVerifier;
	private final OperationalEmailIngestionAuditLogger auditLogger;
	private final ObjectMapper objectMapper;

	public OperationalSignedRequestAuthenticationFilter(
		OperationalEmailIngestionProperties properties,
		OperationalRequestSignatureVerifier signatureVerifier,
		OperationalEmailIngestionAuditLogger auditLogger,
		ObjectMapper objectMapper
	) {
		this.properties = properties;
		this.signatureVerifier = signatureVerifier;
		this.auditLogger = auditLogger;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		if (!request.getRequestURI().startsWith("/api/v1/operations/")) {
			filterChain.doFilter(request, response);
			return;
		}

		CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
		if (!properties.isConfigured()) {
			filterChain.doFilter(wrappedRequest, response);
			return;
		}

		try {
			OperationalRequestVerificationResult verificationResult = signatureVerifier.verify(wrappedRequest);
			UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
				"operational-email-ingestion:" + verificationResult.keyId(),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_OPERATIONAL_EMAIL_INGESTION"))
			);
			authentication.setDetails(verificationResult);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			auditLogger.requestReceived(
				wrappedRequest.getRequestURI(),
				verificationResult.keyId(),
				verificationResult.sourceAccount(),
				verificationResult.nonceFingerprint(),
				verificationResult.bodyHashPrefix()
			);
			filterChain.doFilter(wrappedRequest, response);
		} catch (OperationalRequestValidationException exception) {
			SecurityContextHolder.clearContext();
			auditLogger.requestRejected(wrappedRequest.getRequestURI(), exception);
			writeFailure(response, exception);
		}
	}

	private void writeFailure(HttpServletResponse response, OperationalRequestValidationException exception) throws IOException {
		ApiErrorResponse body;
		if (exception.getReason().httpStatus().is4xxClientError() && exception.getReason().httpStatus().value() == 422) {
			body = ApiErrorResponses.body("BUSINESS_RULE", "Operational payload is invalid");
		} else if (exception.getReason().httpStatus().is4xxClientError() && exception.getReason().httpStatus().value() == 400) {
			body = ApiErrorResponses.body("INVALID_REQUEST", "Operational payload is invalid");
		} else {
			body = ApiErrorResponses.body("UNAUTHORIZED", "Authentication required");
		}
		response.setStatus(exception.getReason().httpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), body);
	}
}
