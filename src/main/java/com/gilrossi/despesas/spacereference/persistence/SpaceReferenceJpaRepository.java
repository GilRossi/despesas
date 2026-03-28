package com.gilrossi.despesas.spacereference.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gilrossi.despesas.spacereference.SpaceReferenceType;

public interface SpaceReferenceJpaRepository extends JpaRepository<SpaceReferenceJpaEntity, Long> {

	@Query("""
		select r
		from SpaceReferenceJpaEntity r
		where r.deletedAt is null
			and r.householdId = :householdId
			and (:type is null or r.type = :type)
			and (:q is null or r.normalizedName like concat('%', :q, '%'))
		order by r.type asc, r.name asc, r.id asc
		""")
	List<SpaceReferenceJpaEntity> findAllByFilters(
		@Param("householdId") Long householdId,
		@Param("type") SpaceReferenceType type,
		@Param("q") String q
	);

	@Query("""
		select r
		from SpaceReferenceJpaEntity r
		where r.deletedAt is null
			and r.householdId = :householdId
			and r.type in :types
			and (:q is null or r.normalizedName like concat('%', :q, '%'))
		order by r.type asc, r.name asc, r.id asc
		""")
	List<SpaceReferenceJpaEntity> findAllByTypeInAndFilters(
		@Param("householdId") Long householdId,
		@Param("types") List<SpaceReferenceType> types,
		@Param("q") String q
	);

	Optional<SpaceReferenceJpaEntity> findByHouseholdIdAndTypeAndNormalizedNameAndDeletedAtIsNull(
		Long householdId,
		SpaceReferenceType type,
		String normalizedName
	);

	Optional<SpaceReferenceJpaEntity> findByIdAndHouseholdIdAndDeletedAtIsNull(Long id, Long householdId);
}
