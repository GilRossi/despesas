package com.gilrossi.despesas.identity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdModuleRepository extends JpaRepository<HouseholdModule, Long> {

	List<HouseholdModule> findAllByHouseholdIdOrderByModuleKeyAsc(Long householdId);

	List<HouseholdModule> findAllByHouseholdIdInOrderByHouseholdIdAscModuleKeyAsc(Collection<Long> householdIds);

	Optional<HouseholdModule> findByHouseholdIdAndModuleKey(Long householdId, HouseholdModuleKey moduleKey);
}
