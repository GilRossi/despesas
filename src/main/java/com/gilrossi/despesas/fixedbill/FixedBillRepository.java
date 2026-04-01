package com.gilrossi.despesas.fixedbill;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FixedBillRepository extends JpaRepository<FixedBill, Long> {
	List<FixedBill> findAllByHouseholdIdAndActiveTrueOrderByCreatedAtDescIdDesc(Long householdId);
}
