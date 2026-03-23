package com.gilrossi.despesas.security;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperationalRequestNonceRecordRepository extends JpaRepository<OperationalRequestNonceRecord, Long> {

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
		delete from OperationalRequestNonceRecord record
		where record.expiresAt <= :now
		""")
	int deleteExpiredBefore(@Param("now") Instant now);
}
