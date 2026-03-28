package com.gilrossi.despesas.spacereference;

import java.util.List;
import java.util.Optional;

public interface SpaceReferenceRepository {

	List<SpaceReference> findAll(Long householdId, SpaceReferenceTypeGroup typeGroup, SpaceReferenceType type, String q);

	Optional<SpaceReference> findById(Long householdId, Long id);

	Optional<SpaceReference> findByTypeAndNormalizedName(Long householdId, SpaceReferenceType type, String normalizedName);

	SpaceReference save(SpaceReference reference);

	void deleteAll();
}
