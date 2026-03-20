package com.gilrossi.despesas.catalog.subcategory.persistence;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SubcategoryJpaRepository extends JpaRepository<SubcategoryJpaEntity, Long> {

	@Query("""
		select s
		from SubcategoryJpaEntity s
		where s.deletedAt is null
			and s.householdId = :householdId
			and (:categoryId is null or s.categoryId = :categoryId)
			and (:q is null or lower(s.name) like lower(concat('%', :q, '%')))
			and (:active is null or s.active = :active)
		""")
	Page<SubcategoryJpaEntity> findAllByFilters(
		@Param("householdId") Long householdId,
		@Param("categoryId") Long categoryId,
		@Param("q") String q,
		@Param("active") Boolean active,
		Pageable pageable
	);

	@Query("""
		select s
		from SubcategoryJpaEntity s
		where s.deletedAt is null
			and s.householdId = :householdId
			and s.id = :id
		""")
	Optional<SubcategoryJpaEntity> findByIdAndHouseholdIdAndDeletedAtIsNull(@Param("householdId") Long householdId, @Param("id") Long id);

	@Query("""
		select case when count(s) > 0 then true else false end
		from SubcategoryJpaEntity s
		where s.deletedAt is null
			and s.householdId = :householdId
			and s.active = true
			and s.categoryId = :categoryId
			and lower(s.name) = lower(:name)
			and (:id is null or s.id <> :id)
		""")
	boolean existsByCategoryIdAndNameIgnoreCaseAndIdNotAndActiveTrue(
		@Param("householdId") Long householdId,
		@Param("categoryId") Long categoryId,
		@Param("name") String name,
		@Param("id") Long id
	);

	@Modifying
	@Transactional
	@Query("""
		update SubcategoryJpaEntity s
		set s.active = false
		where s.deletedAt is null
			and s.householdId = :householdId
			and s.categoryId = :categoryId
		""")
	int desativarPorCategoriaId(@Param("householdId") Long householdId, @Param("categoryId") Long categoryId);

	@Query("""
		select s
		from SubcategoryJpaEntity s
		where s.deletedAt is null
			and s.householdId = :householdId
			and s.active = true
		order by s.categoryId asc, s.name asc, s.id asc
		""")
	List<SubcategoryJpaEntity> findActiveByHouseholdId(@Param("householdId") Long householdId);
}
