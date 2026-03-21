package com.gilrossi.despesas.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class OperationalBearerTokenAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final OperationalEmailIngestionProperties properties;

	public OperationalBearerTokenAuthenticationFilter(OperationalEmailIngestionProperties properties) {
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, jakarta.servlet.FilterChain filterChain)
		throws jakarta.servlet.ServletException, java.io.IOException {
		if (!request.getRequestURI().startsWith("/api/v1/operations/")) {
			filterChain.doFilter(request, response);
			return;
		}
		String configuredToken = properties.token();
		String authorization = request.getHeader(AUTHORIZATION_HEADER);
		if (!StringUtils.hasText(configuredToken) || !StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		String providedToken = authorization.substring(BEARER_PREFIX.length()).trim();
		if (tokenMatches(configuredToken, providedToken)) {
			UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
				"operational-email-ingestion",
				null,
				List.of(new SimpleGrantedAuthority("ROLE_OPERATIONAL_EMAIL_INGESTION"))
			);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		filterChain.doFilter(request, response);
	}

	private boolean tokenMatches(String configuredToken, String providedToken) {
		return MessageDigest.isEqual(
			configuredToken.getBytes(StandardCharsets.UTF_8),
			providedToken.getBytes(StandardCharsets.UTF_8)
		);
	}
}
