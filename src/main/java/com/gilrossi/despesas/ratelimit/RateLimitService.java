package com.gilrossi.despesas.ratelimit;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateLimitService {

	private final RateLimitCounterRepository repository;
	private final Clock clock;

	@Autowired
	public RateLimitService(RateLimitCounterRepository repository) {
		this(repository, Clock.systemUTC());
	}

	RateLimitService(RateLimitCounterRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void enforce(RateLimitScope scope, String scopeKey, int maxRequests, long windowSeconds) {
		Instant now = clock.instant();
		repository.deleteExpiredBefore(now);
		Instant windowStart = floorWindow(now, windowSeconds);
		Instant windowEnd = windowStart.plusSeconds(windowSeconds);
		enforce(scope, normalize(scopeKey), maxRequests, windowSeconds, windowStart, windowEnd, now, true);
	}

	private void enforce(
		RateLimitScope scope,
		String scopeKey,
		int maxRequests,
		long windowSeconds,
		Instant windowStart,
		Instant windowEnd,
		Instant now,
		boolean retryOnConflict
	) {
		try {
			RateLimitCounter counter = repository.findByScopeAndScopeKeyAndWindowStartForUpdate(
				scope.name(),
				scopeKey,
				windowStart
			).orElseGet(() -> repository.saveAndFlush(new RateLimitCounter(
				scope.name(),
				scopeKey,
				windowStart,
				windowEnd,
				0
			)));
			if (counter.getRequestCount() >= maxRequests) {
				throw new RateLimitExceededException(
					scope,
					maxRequests,
					windowSeconds,
					Math.max(1L, windowEnd.getEpochSecond() - now.getEpochSecond())
				);
			}
			counter.increment();
		} catch (DataIntegrityViolationException exception) {
			if (retryOnConflict) {
				enforce(scope, scopeKey, maxRequests, windowSeconds, windowStart, windowEnd, now, false);
				return;
			}
			throw exception;
		}
	}

	private Instant floorWindow(Instant now, long windowSeconds) {
		long epochSeconds = now.getEpochSecond();
		return Instant.ofEpochSecond(epochSeconds - Math.floorMod(epochSeconds, windowSeconds));
	}

	private String normalize(String scopeKey) {
		return scopeKey == null || scopeKey.isBlank() ? "-" : scopeKey.trim();
	}
}
