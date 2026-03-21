package com.gilrossi.despesas.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.operational.email-ingestion")
public record OperationalEmailIngestionProperties(
	String token
) {
}
