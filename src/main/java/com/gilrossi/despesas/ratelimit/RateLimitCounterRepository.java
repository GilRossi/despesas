package com.gilrossi.despesas.ratelimit;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RateLimitCounterRepository extends JpaRepository<RateLimitCounter, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select counter
		from RateLimitCounter counter
		where counter.scope = :scope
		  and counter.scopeKey = :scopeKey
		  and counter.windowStart = :windowStart
		""")
	Optional<RateLimitCounter> findByScopeAndScopeKeyAndWindowStartForUpdate(
		@Param("scope") String scope,
		@Param("scopeKey") String scopeKey,
		@Param("windowStart") Instant windowStart
	);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
		delete from RateLimitCounter counter
		where counter.windowEnd <= :now
		""")
	int deleteExpiredBefore(@Param("now") Instant now);
}
