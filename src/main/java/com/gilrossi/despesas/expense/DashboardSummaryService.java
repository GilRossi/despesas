package com.gilrossi.despesas.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.payment.Payment;
import com.gilrossi.despesas.payment.PaymentRepository;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class DashboardSummaryService {

	private final ExpenseRepository expenseRepository;
	private final PaymentRepository paymentRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;
	private final ExpenseStatusCalculator expenseStatusCalculator;

	public DashboardSummaryService(
		ExpenseRepository expenseRepository,
		PaymentRepository paymentRepository,
		CurrentHouseholdProvider currentHouseholdProvider,
		ExpenseStatusCalculator expenseStatusCalculator
	) {
		this.expenseRepository = expenseRepository;
		this.paymentRepository = paymentRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
		this.expenseStatusCalculator = expenseStatusCalculator;
	}

	@Transactional(readOnly = true)
	public DashboardSummaryResponse resumir() {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		List<Expense> expenses = expenseRepository.findAllByHouseholdId(householdId);
		List<Long> expenseIds = expenses.stream().map(Expense::getId).toList();
		Map<Long, List<Payment>> paymentsByExpenseId = expenseIds.isEmpty()
			? Map.of()
			: paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(expenseIds).stream()
				.collect(Collectors.groupingBy(Payment::getExpenseId, LinkedHashMap::new, Collectors.toList()));

		BigDecimal totalAmount = expenses.stream()
			.map(Expense::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal paidAmount = paymentsByExpenseId.values().stream()
			.flatMap(List::stream)
			.map(Payment::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal remainingAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO);

		List<DashboardExpenseSnapshot> snapshots = expenses.stream()
			.map(expense -> snapshot(expense, paymentsByExpenseId.getOrDefault(expense.getId(), List.of())))
			.toList();
		long overdueCount = snapshots.stream().filter(snapshot -> snapshot.status() == ExpenseStatus.VENCIDA).count();
		BigDecimal overdueAmount = snapshots.stream()
			.filter(snapshot -> snapshot.status() == ExpenseStatus.VENCIDA)
			.map(DashboardExpenseSnapshot::remainingAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		long openCount = snapshots.stream().filter(snapshot -> isOpen(snapshot.status())).count();
		BigDecimal openAmount = snapshots.stream()
			.filter(snapshot -> isOpen(snapshot.status()))
			.map(DashboardExpenseSnapshot::remainingAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		List<DashboardStatusSummary> statuses = snapshots.stream()
			.collect(Collectors.groupingBy(DashboardExpenseSnapshot::status, LinkedHashMap::new, Collectors.toList()))
			.entrySet().stream()
			.map(entry -> new DashboardStatusSummary(
				entry.getKey(),
				entry.getValue().size(),
				entry.getValue().stream().map(DashboardExpenseSnapshot::remainingAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
			))
			.toList();

		return new DashboardSummaryResponse(
			householdId,
			expenses.size(),
			totalAmount,
			paidAmount,
			remainingAmount,
			overdueCount,
			overdueAmount,
			openCount,
			openAmount,
			statuses
		);
	}

	private DashboardExpenseSnapshot snapshot(Expense expense, List<Payment> payments) {
		BigDecimal paidAmount = payments.stream()
			.map(Payment::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal remainingAmount = expense.getAmount().subtract(paidAmount).max(BigDecimal.ZERO);
		ExpenseStatus status = expenseStatusCalculator.calcular(expense.getAmount(), paidAmount, expense.getDueDate(), LocalDate.now());
		return new DashboardExpenseSnapshot(remainingAmount, status);
	}

	private boolean isOpen(ExpenseStatus status) {
		return status == ExpenseStatus.PREVISTA || status == ExpenseStatus.ABERTA || status == ExpenseStatus.PARCIALMENTE_PAGA;
	}

	private record DashboardExpenseSnapshot(BigDecimal remainingAmount, ExpenseStatus status) {
	}
}
