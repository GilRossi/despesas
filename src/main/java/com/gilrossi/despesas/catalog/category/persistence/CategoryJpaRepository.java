package com.gilrossi.despesas.catalog.category.persistence;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

	@Query("""
		select c
		from CategoryJpaEntity c
		where c.deletedAt is null
			and c.householdId = :householdId
			and (:q is null or lower(c.name) like lower(concat('%', :q, '%')))
			and (:active is null or c.active = :active)
		""")
	Page<CategoryJpaEntity> findAllByFilters(@Param("householdId") Long householdId, @Param("q") String q, @Param("active") Boolean active, Pageable pageable);

	@Query("""
		select c
		from CategoryJpaEntity c
		where c.deletedAt is null
			and c.householdId = :householdId
			and c.id = :id
		""")
	Optional<CategoryJpaEntity> findByIdAndHouseholdIdAndDeletedAtIsNull(@Param("householdId") Long householdId, @Param("id") Long id);

	@Query("""
		select c
		from CategoryJpaEntity c
		where c.deletedAt is null
			and c.householdId = :householdId
			and lower(c.name) = lower(:name)
		""")
	Optional<CategoryJpaEntity> findByNameIgnoreCaseAndHouseholdIdAndDeletedAtIsNull(@Param("householdId") Long householdId, @Param("name") String name);

	@Query("""
		select case when count(c) > 0 then true else false end
		from CategoryJpaEntity c
		where c.deletedAt is null
			and c.householdId = :householdId
			and c.active = true
			and lower(c.name) = lower(:name)
			and (:id is null or c.id <> :id)
		""")
	boolean existsByNameIgnoreCaseAndIdNotAndHouseholdIdAndActiveTrue(@Param("householdId") Long householdId, @Param("name") String name, @Param("id") Long id);

	@Query("""
		select c
		from CategoryJpaEntity c
		where c.deletedAt is null
			and c.householdId = :householdId
			and c.active = true
		order by c.name asc, c.id asc
		""")
	List<CategoryJpaEntity> findActiveByHouseholdId(@Param("householdId") Long householdId);
}
