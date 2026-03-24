package com.gilrossi.despesas.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.audit.AuditTrailProperties;
import com.gilrossi.despesas.identity.PlatformAdminBootstrapProperties;
import com.gilrossi.despesas.ratelimit.AbuseProtectionService;
import com.gilrossi.despesas.ratelimit.RateLimitProperties;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({
	ApiSecurityProperties.class,
	OperationalEmailIngestionProperties.class,
	PlatformAdminBootstrapProperties.class,
	AuditTrailProperties.class,
	RateLimitProperties.class
})
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	ApiTokenService apiTokenService(
		ObjectMapper objectMapper,
		ApiSecurityProperties properties
	) {
		return new ApiTokenService(objectMapper, properties.tokenSecret());
	}

	@Bean
	ApiBearerTokenAuthenticationFilter apiBearerTokenAuthenticationFilter(
		ApiTokenService apiTokenService,
		ApiAuthenticationEntryPoint authenticationEntryPoint
	) {
		return new ApiBearerTokenAuthenticationFilter(apiTokenService, authenticationEntryPoint);
	}

	@Bean
	OperationalSignedRequestAuthenticationFilter operationalSignedRequestAuthenticationFilter(
		OperationalEmailIngestionProperties properties,
		OperationalRequestSignatureVerifier signatureVerifier,
		OperationalEmailIngestionAuditLogger auditLogger,
		AbuseProtectionService abuseProtectionService,
		ObjectMapper objectMapper
	) {
		return new OperationalSignedRequestAuthenticationFilter(
			properties,
			signatureVerifier,
			auditLogger,
			abuseProtectionService,
			objectMapper
		);
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
	}

	@Bean
	@Order(1)
	SecurityFilterChain apiFilterChain(
		HttpSecurity http,
		ApiAuthenticationEntryPoint authenticationEntryPoint,
		ApiAccessDeniedHandler accessDeniedHandler,
		ApiBearerTokenAuthenticationFilter apiBearerTokenAuthenticationFilter,
		OperationalSignedRequestAuthenticationFilter operationalSignedRequestAuthenticationFilter
	) throws Exception {
		http.securityMatcher("/api/**")
			.cors(Customizer.withDefaults())
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
				.requestMatchers("/api/v1/operations/**").hasRole("OPERATIONAL_EMAIL_INGESTION")
				.requestMatchers("/api/v1/auth/me").authenticated()
				.anyRequest().authenticated())
			.addFilterBefore(operationalSignedRequestAuthenticationFilter, BasicAuthenticationFilter.class)
			.addFilterAfter(apiBearerTokenAuthenticationFilter, OperationalSignedRequestAuthenticationFilter.class)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler));
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(ApiSecurityProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> allowedOriginPatterns = properties.corsAllowedOriginPatterns();
		if (allowedOriginPatterns == null) {
			allowedOriginPatterns = List.of();
		}
		configuration.setAllowedOriginPatterns(allowedOriginPatterns);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of("Retry-After"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	@Bean
	@Order(2)
	SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.GET, "/**").permitAll()
				.requestMatchers(HttpMethod.HEAD, "/**").permitAll()
				.anyRequest().denyAll());
		return http.build();
	}
}
