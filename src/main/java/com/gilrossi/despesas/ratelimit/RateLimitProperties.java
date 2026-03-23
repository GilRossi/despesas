package com.gilrossi.despesas.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limits")
public class RateLimitProperties {

	private final Rule authLogin = new Rule(5, 300);
	private final Rule authRefresh = new Rule(10, 300);
	private final Rule assistantQuery = new Rule(15, 60);
	private final Rule operationalEmailIngestion = new Rule(120, 60);

	public Rule getAuthLogin() {
		return authLogin;
	}

	public Rule getAuthRefresh() {
		return authRefresh;
	}

	public Rule getAssistantQuery() {
		return assistantQuery;
	}

	public Rule getOperationalEmailIngestion() {
		return operationalEmailIngestion;
	}

	public static class Rule {

		private int maxRequests;
		private long windowSeconds;

		public Rule() {
		}

		public Rule(int maxRequests, long windowSeconds) {
			this.maxRequests = maxRequests;
			this.windowSeconds = windowSeconds;
		}

		public int getMaxRequests() {
			return maxRequests;
		}

		public void setMaxRequests(int maxRequests) {
			this.maxRequests = maxRequests;
		}

		public long getWindowSeconds() {
			return windowSeconds;
		}

		public void setWindowSeconds(long windowSeconds) {
			this.windowSeconds = windowSeconds;
		}
	}
}
