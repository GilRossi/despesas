package com.gilrossi.despesas.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.gilrossi.despesas.DespesasApplication;

class ApiTokenSecretConfigurationTest {

	@Test
	void deve_falhar_no_startup_quando_token_secret_nao_estiver_configurado() {
		assertThatThrownBy(() -> new SpringApplicationBuilder(DespesasApplication.class)
			.properties(
				"spring.config.name=missing-token-secret-config",
				"spring.main.web-application-type=servlet",
				"server.port=0",
				"spring.flyway.enabled=false",
				"spring.datasource.url=jdbc:h2:mem:tokenSecretStartup;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
				"spring.datasource.username=sa",
				"spring.datasource.password=",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
				"spring.jpa.hibernate.ddl-auto=create-drop"
			)
			.run())
			.hasStackTraceContaining("app.security.token-secret must be configured");
	}
}
