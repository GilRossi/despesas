package com.gilrossi.despesas.expense;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

	default Page<Expense> findAllByFilters(
		Long householdId,
		String qPattern,
		ExpenseContext context,
		Long categoryId,
		Long subcategoryId,
		ExpenseStatus status,
		Boolean overdue,
		LocalDate dueDateFrom,
		LocalDate dueDateTo,
		Boolean hasPayments,
		Pageable pageable
	) {
		return findAllByFilters(
			householdId,
			toQueryPattern(qPattern),
			context,
			categoryId,
			subcategoryId,
			status == null ? null : status.name(),
			overdue,
			dueDateFrom,
			dueDateTo,
			hasPayments,
			LocalDate.now(),
			pageable
		);
	}

	private String toQueryPattern(String q) {
		if (q == null || q.isBlank()) {
			return null;
		}
		return "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
	}

	@Query("""
		select e
		from Expense e
		left join Payment p on p.expenseId = e.id and p.deletedAt is null
		where e.deletedAt is null
			and e.householdId = :householdId
			and (:qPattern is null or lower(e.description) like :qPattern)
			and (:context is null or e.context = :context)
			and (:categoryId is null or e.categoryId = :categoryId)
			and (:subcategoryId is null or e.subcategoryId = :subcategoryId)
			and (:dueDateFrom is null or e.dueDate >= :dueDateFrom)
			and (:dueDateTo is null or e.dueDate <= :dueDateTo)
		group by e
		having (:hasPayments is null
				or (:hasPayments = true and count(p) > 0)
				or (:hasPayments = false and count(p) = 0))
			and (:overdue is null
				or (:overdue = true and e.dueDate < :today and coalesce(sum(p.amount), 0) < e.amount)
				or (:overdue = false and not (e.dueDate < :today and coalesce(sum(p.amount), 0) < e.amount)))
			and (:status is null
				or (:status = 'PAGA' and coalesce(sum(p.amount), 0) >= e.amount)
				or (:status = 'PARCIALMENTE_PAGA' and coalesce(sum(p.amount), 0) > 0 and coalesce(sum(p.amount), 0) < e.amount)
				or (:status = 'PREVISTA' and coalesce(sum(p.amount), 0) = 0 and e.dueDate > :today)
				or (:status = 'ABERTA' and coalesce(sum(p.amount), 0) = 0 and e.dueDate = :today)
				or (:status = 'VENCIDA' and coalesce(sum(p.amount), 0) = 0 and e.dueDate < :today))
		""")
	Page<Expense> findAllByFilters(
		@Param("householdId") Long householdId,
		@Param("qPattern") String qPattern,
		@Param("context") ExpenseContext context,
		@Param("categoryId") Long categoryId,
		@Param("subcategoryId") Long subcategoryId,
		@Param("status") String status,
		@Param("overdue") Boolean overdue,
		@Param("dueDateFrom") LocalDate dueDateFrom,
		@Param("dueDateTo") LocalDate dueDateTo,
		@Param("hasPayments") Boolean hasPayments,
		@Param("today") LocalDate today,
		Pageable pageable
	);

	@Query("""
		select e
		from Expense e
		where e.deletedAt is null
			and e.id = :id
			and e.householdId = :householdId
		""")
	Optional<Expense> findByIdAndHouseholdId(@Param("id") Long id, @Param("householdId") Long householdId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select e
		from Expense e
		where e.deletedAt is null
			and e.id = :id
			and e.householdId = :householdId
		""")
	Optional<Expense> findByIdAndHouseholdIdForUpdate(@Param("id") Long id, @Param("householdId") Long householdId);

	@Query("""
		select e
		from Expense e
		where e.deletedAt is null
			and e.householdId = :householdId
		order by e.dueDate desc, e.id desc
		""")
	List<Expense> findAllByHouseholdId(@Param("householdId") Long householdId);

	@Query("""
		select e
		from Expense e
		where e.deletedAt is null
			and e.householdId = :householdId
			and e.dueDate between :from and :to
		order by e.dueDate desc, e.id desc
		""")
	List<Expense> findAllByHouseholdIdAndDueDateBetween(
		@Param("householdId") Long householdId,
		@Param("from") LocalDate from,
		@Param("to") LocalDate to
	);

	@Query("""
		select e
		from Expense e
		where e.deletedAt is null
			and e.householdId = :householdId
			and e.dueDate >= :from
		order by e.dueDate desc, e.id desc
		""")
	List<Expense> findAllByHouseholdIdAndDueDateGreaterThanEqual(
		@Param("householdId") Long householdId,
		@Param("from") LocalDate from
	);

	@Query("""
		select case when count(e) > 0 then true else false end
		from Expense e
		where e.deletedAt is null
			and e.householdId = :householdId
			and e.subcategoryId = :subcategoryId
		""")
	boolean existsByHouseholdIdAndSubcategoryId(@Param("householdId") Long householdId, @Param("subcategoryId") Long subcategoryId);
}
