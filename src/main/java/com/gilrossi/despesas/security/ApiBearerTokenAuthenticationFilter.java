package com.gilrossi.despesas.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiBearerTokenAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final ApiTokenService apiTokenService;
	private final AuthenticationEntryPoint authenticationEntryPoint;

	public ApiBearerTokenAuthenticationFilter(ApiTokenService apiTokenService, ApiAuthenticationEntryPoint authenticationEntryPoint) {
		this.apiTokenService = apiTokenService;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String authorization = request.getHeader("Authorization");
		if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
			try {
				AuthenticatedHouseholdUser principal = apiTokenService.authenticateAccessToken(authorization.substring(BEARER_PREFIX.length()));
				var authentication = UsernamePasswordAuthenticationToken.authenticated(
					principal,
					null,
					principal.getAuthorities()
				);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (org.springframework.security.core.AuthenticationException exception) {
				SecurityContextHolder.clearContext();
				authenticationEntryPoint.commence(request, response, exception);
				return;
			}
		}
		filterChain.doFilter(request, response);
	}
}
