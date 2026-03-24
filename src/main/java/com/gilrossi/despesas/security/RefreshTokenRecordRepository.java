package com.gilrossi.despesas.security;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRecordRepository extends JpaRepository<RefreshTokenRecord, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select token
		from RefreshTokenRecord token
		where token.tokenId = :tokenId
		""")
	Optional<RefreshTokenRecord> findByTokenIdForUpdate(@Param("tokenId") String tokenId);

	List<RefreshTokenRecord> findByFamilyIdAndRevokedAtIsNull(String familyId);

	List<RefreshTokenRecord> findByUserIdAndRevokedAtIsNull(Long userId);
}
