package com.gilrossi.despesas.security;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.operational.email-ingestion")
public class OperationalEmailIngestionProperties {

	private String keyId;
	private String secret;
	private String previousKeyId;
	private String previousSecret;
	private long maxClockSkewSeconds = 300L;
	private long nonceTtlSeconds = 86_400L;

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getPreviousKeyId() {
		return previousKeyId;
	}

	public void setPreviousKeyId(String previousKeyId) {
		this.previousKeyId = previousKeyId;
	}

	public String getPreviousSecret() {
		return previousSecret;
	}

	public void setPreviousSecret(String previousSecret) {
		this.previousSecret = previousSecret;
	}

	public long getMaxClockSkewSeconds() {
		return maxClockSkewSeconds;
	}

	public void setMaxClockSkewSeconds(long maxClockSkewSeconds) {
		this.maxClockSkewSeconds = maxClockSkewSeconds;
	}

	public long getNonceTtlSeconds() {
		return nonceTtlSeconds;
	}

	public void setNonceTtlSeconds(long nonceTtlSeconds) {
		this.nonceTtlSeconds = nonceTtlSeconds;
	}

	public boolean isConfigured() {
		return StringUtils.hasText(keyId) && StringUtils.hasText(secret);
	}

	public Map<String, String> signingSecrets() {
		Map<String, String> secrets = new LinkedHashMap<>();
		if (StringUtils.hasText(keyId) && StringUtils.hasText(secret)) {
			secrets.put(keyId.trim(), secret);
		}
		if (StringUtils.hasText(previousKeyId) && StringUtils.hasText(previousSecret)) {
			secrets.put(previousKeyId.trim(), previousSecret);
		}
		return secrets;
	}
}
