package com.gilrossi.despesas.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({ApiSecurityProperties.class, OperationalEmailIngestionProperties.class})
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
	OperationalBearerTokenAuthenticationFilter operationalBearerTokenAuthenticationFilter(
		OperationalEmailIngestionProperties properties
	) {
		return new OperationalBearerTokenAuthenticationFilter(properties);
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	@Order(1)
	SecurityFilterChain apiFilterChain(
		HttpSecurity http,
		ApiAuthenticationEntryPoint authenticationEntryPoint,
		ApiAccessDeniedHandler accessDeniedHandler,
		ApiBearerTokenAuthenticationFilter apiBearerTokenAuthenticationFilter,
		OperationalBearerTokenAuthenticationFilter operationalBearerTokenAuthenticationFilter
	) throws Exception {
		http.securityMatcher("/api/**")
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
				.requestMatchers("/api/v1/operations/**").hasRole("OPERATIONAL_EMAIL_INGESTION")
				.requestMatchers("/api/v1/auth/me").authenticated()
				.anyRequest().authenticated())
			.addFilterBefore(operationalBearerTokenAuthenticationFilter, BasicAuthenticationFilter.class)
			.addFilterAfter(apiBearerTokenAuthenticationFilter, OperationalBearerTokenAuthenticationFilter.class)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler));
		return http.build();
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
