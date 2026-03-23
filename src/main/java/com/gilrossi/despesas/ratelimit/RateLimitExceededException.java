package com.gilrossi.despesas.ratelimit;

public class RateLimitExceededException extends RuntimeException {

	private final RateLimitScope scope;
	private final int maxRequests;
	private final long windowSeconds;
	private final long retryAfterSeconds;

	public RateLimitExceededException(
		RateLimitScope scope,
		int maxRequests,
		long windowSeconds,
		long retryAfterSeconds
	) {
		super("Too many requests");
		this.scope = scope;
		this.maxRequests = maxRequests;
		this.windowSeconds = windowSeconds;
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public RateLimitScope getScope() {
		return scope;
	}

	public int getMaxRequests() {
		return maxRequests;
	}

	public long getWindowSeconds() {
		return windowSeconds;
	}

	public long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}
}
