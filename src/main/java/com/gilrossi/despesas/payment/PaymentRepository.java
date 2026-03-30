package com.gilrossi.despesas.payment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	@Query("""
		select p
		from Payment p
		where p.deletedAt is null
			and p.id = :id
		""")
	Optional<Payment> findActiveById(@Param("id") Long id);

	@Query("""
		select p
		from Payment p
		where p.deletedAt is null
			and p.expenseId = :expenseId
		order by p.paidAt desc, p.id desc
		""")
	List<Payment> findByExpenseIdOrderByPaidAtDescIdDesc(@Param("expenseId") Long expenseId);

	@Query("""
		select p
		from Payment p
		where p.deletedAt is null
			and p.expenseId = :expenseId
		order by p.paidAt desc, p.id desc
		""")
	Page<Payment> findByExpenseIdOrderByPaidAtDescIdDesc(@Param("expenseId") Long expenseId, Pageable pageable);

	@Query("""
		select p
		from Payment p
		where p.deletedAt is null
			and p.expenseId in :expenseIds
		order by p.expenseId asc, p.paidAt desc, p.id desc
		""")
	List<Payment> findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(@Param("expenseIds") Collection<Long> expenseIds);

	default List<Payment> findByExpenseId(Long expenseId) {
		return findByExpenseIdOrderByPaidAtDescIdDesc(expenseId);
	}
}
