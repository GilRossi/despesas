package com.gilrossi.despesas.identity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdRepository extends JpaRepository<Household, Long> {

	List<Household> findAllByDeletedAtIsNullOrderByCreatedAtDescIdDesc();

	Optional<Household> findByIdAndDeletedAtIsNull(Long id);
}
