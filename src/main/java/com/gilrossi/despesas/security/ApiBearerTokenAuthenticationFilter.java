package com.gilrossi.despesas.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiBearerTokenAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final ApiTokenService apiTokenService;
	private final HouseholdUserDetailsService householdUserDetailsService;
	private final AuthenticationEntryPoint authenticationEntryPoint;

	public ApiBearerTokenAuthenticationFilter(
		ApiTokenService apiTokenService,
		HouseholdUserDetailsService householdUserDetailsService,
		ApiAuthenticationEntryPoint authenticationEntryPoint
	) {
		this.apiTokenService = apiTokenService;
		this.householdUserDetailsService = householdUserDetailsService;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() != null) {
			filterChain.doFilter(request, response);
			return;
		}
		String authorization = request.getHeader("Authorization");
		if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
			try {
				ApiTokenService.AuthenticatedApiToken token = apiTokenService.authenticateAccessToken(authorization.substring(BEARER_PREFIX.length()));
				AuthenticatedHouseholdUser principal = householdUserDetailsService.loadUserById(token.principal().getUserId());
				if (principal.getCredentialsUpdatedAt().isAfter(token.issuedAt())) {
					throw new BadCredentialsException("Authentication failed");
				}
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
