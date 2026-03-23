package com.gilrossi.despesas.ratelimit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "rate_limit_counters",
	uniqueConstraints = @UniqueConstraint(
		name = "uq_rate_limit_counters_scope_key_window",
		columnNames = {"scope", "scope_key", "window_start"}
	),
	indexes = {
		@Index(name = "idx_rate_limit_counters_window_end", columnList = "window_end"),
		@Index(name = "idx_rate_limit_counters_scope_key", columnList = "scope,scope_key")
	}
)
public class RateLimitCounter {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "scope", nullable = false, length = 64)
	private String scope;

	@Column(name = "scope_key", nullable = false, length = 180)
	private String scopeKey;

	@Column(name = "window_start", nullable = false)
	private Instant windowStart;

	@Column(name = "window_end", nullable = false)
	private Instant windowEnd;

	@Column(name = "request_count", nullable = false)
	private int requestCount;

	protected RateLimitCounter() {
	}

	public RateLimitCounter(String scope, String scopeKey, Instant windowStart, Instant windowEnd, int requestCount) {
		this.scope = scope;
		this.scopeKey = scopeKey;
		this.windowStart = windowStart;
		this.windowEnd = windowEnd;
		this.requestCount = requestCount;
	}

	public int getRequestCount() {
		return requestCount;
	}

	public Instant getWindowEnd() {
		return windowEnd;
	}

	public void increment() {
		requestCount += 1;
	}
}
