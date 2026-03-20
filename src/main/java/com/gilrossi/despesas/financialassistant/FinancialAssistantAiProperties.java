package com.gilrossi.despesas.financialassistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.financial-assistant.ai")
public class FinancialAssistantAiProperties {

	private boolean enabled;
	private String apiKey;
	private String baseUrl = "https://api.deepseek.com/v1";
	private String model = "deepseek-chat";
	private boolean logRequests;
	private boolean logResponses;
	private double temperature = 0.1d;
	private int maxCompletionTokens = 220;
	private int timeoutSeconds = 20;
	private int maxRetries = 1;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public boolean isLogRequests() {
		return logRequests;
	}

	public void setLogRequests(boolean logRequests) {
		this.logRequests = logRequests;
	}

	public boolean isLogResponses() {
		return logResponses;
	}

	public void setLogResponses(boolean logResponses) {
		this.logResponses = logResponses;
	}

	public double getTemperature() {
		return temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public int getMaxCompletionTokens() {
		return maxCompletionTokens;
	}

	public void setMaxCompletionTokens(int maxCompletionTokens) {
		this.maxCompletionTokens = maxCompletionTokens;
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public boolean isConfigured() {
		return StringUtils.hasText(apiKey);
	}
}
