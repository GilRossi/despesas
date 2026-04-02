package com.gilrossi.despesas.fixedbill;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface FixedBillRepository extends JpaRepository<FixedBill, Long> {
	List<FixedBill> findAllByHouseholdIdAndActiveTrueOrderByCreatedAtDescIdDesc(Long householdId);

	@Query("""
		select f
		from FixedBill f
		where f.id = :id
			and f.householdId = :householdId
			and f.active = true
		""")
	Optional<FixedBill> findActiveByIdAndHouseholdId(@Param("id") Long id, @Param("householdId") Long householdId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select f
		from FixedBill f
		where f.id = :id
			and f.householdId = :householdId
			and f.active = true
		""")
	Optional<FixedBill> findActiveByIdAndHouseholdIdForUpdate(@Param("id") Long id, @Param("householdId") Long householdId);
}
