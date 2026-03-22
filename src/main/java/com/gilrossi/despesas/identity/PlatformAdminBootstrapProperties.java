package com.gilrossi.despesas.identity;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.bootstrap.platform-admin")
public record PlatformAdminBootstrapProperties(
	boolean enabled,
	String name,
	String email,
	String password
) {
}
