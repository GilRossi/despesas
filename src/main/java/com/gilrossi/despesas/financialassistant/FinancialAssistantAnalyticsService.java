package com.gilrossi.despesas.financialassistant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.payment.Payment;
import com.gilrossi.despesas.payment.PaymentRepository;
@Service
public class FinancialAssistantAnalyticsService {

	private static final int DEFAULT_TOP_LIMIT = 5;
	private static final int RECURRING_MONTH_WINDOW = 6;

	private final ExpenseRepository expenseRepository;
	private final PaymentRepository paymentRepository;
	private final FinancialAssistantAccessContextProvider accessContextProvider;

	public FinancialAssistantAnalyticsService(
		ExpenseRepository expenseRepository,
		PaymentRepository paymentRepository,
		FinancialAssistantAccessContextProvider accessContextProvider
	) {
		this.expenseRepository = expenseRepository;
		this.paymentRepository = paymentRepository;
		this.accessContextProvider = accessContextProvider;
	}

	@Transactional(readOnly = true)
	public FinancialAssistantPeriodSummaryResponse summarize(LocalDate from, LocalDate to) {
		return summarize(accessContextProvider.requireContext(), from, to);
	}

	@Transactional(readOnly = true)
	FinancialAssistantPeriodSummaryResponse summarize(FinancialAssistantAccessContext context, LocalDate from, LocalDate to) {
		FinancialAssistantDateRange range = FinancialAssistantSupport.resolveRange(from, to);
		PeriodSnapshot snapshot = snapshot(context, range);
		List<CategorySpendingResponse> categories = categoryTotals(snapshot.expenses()).stream()
			.sorted(Comparator.comparing(CategorySpendingResponse::totalAmount).reversed().thenComparing(CategorySpendingResponse::categoryName))
			.toList();
		List<TopExpenseResponse> topExpenses = topExpenses(snapshot.expenses(), DEFAULT_TOP_LIMIT);
		CategorySpendingResponse highestCategory = categories.isEmpty() ? null : categories.getFirst();
		return new FinancialAssistantPeriodSummaryResponse(
			range.from(),
			range.to(),
			snapshot.expenses().size(),
			snapshot.totalAmount(),
			snapshot.paidAmount(),
			snapshot.remainingAmount(),
			highestCategory == null ? null : highestCategory.categoryName(),
			categories,
			topExpenses
		);
	}

	@Transactional(readOnly = true)
	public FinancialAssistantKpisResponse kpis(LocalDate from, LocalDate to) {
		return kpis(accessContextProvider.requireContext(), from, to);
	}

	@Transactional(readOnly = true)
	FinancialAssistantKpisResponse kpis(FinancialAssistantAccessContext context, LocalDate from, LocalDate to) {
		FinancialAssistantPeriodSummaryResponse summary = summarize(context, from, to);
		BigDecimal averageExpense = summary.totalExpenses() == 0
			? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
			: summary.totalAmount().divide(BigDecimal.valueOf(summary.totalExpenses()), 2, RoundingMode.HALF_UP);
		BigDecimal largestExpense = summary.topExpenses().isEmpty() ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : summary.topExpenses().getFirst().amount();
		BigDecimal highestCategoryShare = summary.categoryTotals().isEmpty() ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : summary.categoryTotals().getFirst().sharePercentage();
		return new FinancialAssistantKpisResponse(List.of(
			new FinancialAssistantKpiResponse("TOTAL_SPENT", "Total no período", summary.totalAmount(), "Soma de despesas registradas no período"),
			new FinancialAssistantKpiResponse("TOTAL_PAID", "Total pago", summary.paidAmount(), "Pagamentos já lançados para as despesas do período"),
			new FinancialAssistantKpiResponse("TOTAL_PENDING", "Saldo pendente", summary.remainingAmount(), "Valor ainda pendente nas despesas do período"),
			new FinancialAssistantKpiResponse("AVERAGE_EXPENSE", "Despesa média", averageExpense, "Média por lançamento no período"),
			new FinancialAssistantKpiResponse("LARGEST_EXPENSE", "Maior despesa", largestExpense, "Maior lançamento individual do período"),
			new FinancialAssistantKpiResponse("HIGHEST_CATEGORY_SHARE", "Maior participação por categoria", highestCategoryShare, "Participação percentual da categoria que mais pesa no período")
		));
	}

	@Transactional(readOnly = true)
	public MonthComparisonResponse compareMonths(YearMonth referenceMonth) {
		return compareMonths(accessContextProvider.requireContext(), referenceMonth);
	}

	@Transactional(readOnly = true)
	MonthComparisonResponse compareMonths(FinancialAssistantAccessContext context, YearMonth referenceMonth) {
		FinancialAssistantPeriodSummaryResponse current = summarize(context, referenceMonth.atDay(1), referenceMonth.atEndOfMonth());
		YearMonth previousMonth = referenceMonth.minusMonths(1);
		FinancialAssistantPeriodSummaryResponse previous = summarize(context, previousMonth.atDay(1), previousMonth.atEndOfMonth());
		BigDecimal deltaAmount = current.totalAmount().subtract(previous.totalAmount());
		BigDecimal deltaPercentage = FinancialAssistantSupport.percentageChange(current.totalAmount(), previous.totalAmount());
		return new MonthComparisonResponse(
			FinancialAssistantSupport.monthLabel(referenceMonth),
			current.totalAmount(),
			FinancialAssistantSupport.monthLabel(previousMonth),
			previous.totalAmount(),
			deltaAmount,
			deltaPercentage
		);
	}

	@Transactional(readOnly = true)
	public List<IncreaseAlertResponse> increaseAlerts(YearMonth referenceMonth) {
		return increaseAlerts(accessContextProvider.requireContext(), referenceMonth);
	}

	@Transactional(readOnly = true)
	List<IncreaseAlertResponse> increaseAlerts(FinancialAssistantAccessContext context, YearMonth referenceMonth) {
		FinancialAssistantPeriodSummaryResponse current = summarize(context, referenceMonth.atDay(1), referenceMonth.atEndOfMonth());
		FinancialAssistantPeriodSummaryResponse previous = summarize(
			context,
			referenceMonth.minusMonths(1).atDay(1),
			referenceMonth.minusMonths(1).atEndOfMonth()
		);
		Map<String, CategorySpendingResponse> previousByName = previous.categoryTotals().stream()
			.collect(Collectors.toMap(category -> FinancialAssistantSupport.normalizeText(category.categoryName()), category -> category, (left, right) -> left, LinkedHashMap::new));
		return current.categoryTotals().stream()
			.map(category -> {
				CategorySpendingResponse previousCategory = previousByName.get(FinancialAssistantSupport.normalizeText(category.categoryName()));
				BigDecimal previousAmount = previousCategory == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : previousCategory.totalAmount();
				BigDecimal deltaAmount = category.totalAmount().subtract(previousAmount);
				BigDecimal deltaPercentage = FinancialAssistantSupport.percentageChange(category.totalAmount(), previousAmount);
				return new IncreaseAlertResponse(category.categoryName(), category.totalAmount(), previousAmount, deltaAmount, deltaPercentage);
			})
			.filter(alert -> alert.deltaAmount().compareTo(new BigDecimal("50.00")) >= 0 || alert.deltaPercentage().compareTo(new BigDecimal("20.00")) >= 0)
			.filter(alert -> alert.currentAmount().compareTo(alert.previousAmount()) > 0)
			.sorted(Comparator.comparing(IncreaseAlertResponse::deltaAmount).reversed().thenComparing(IncreaseAlertResponse::categoryName))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<RecurringExpenseResponse> recurringExpenses(YearMonth referenceMonth) {
		return recurringExpenses(accessContextProvider.requireContext(), referenceMonth);
	}

	@Transactional(readOnly = true)
	List<RecurringExpenseResponse> recurringExpenses(FinancialAssistantAccessContext context, YearMonth referenceMonth) {
		Long householdId = context.householdId();
		LocalDate from = referenceMonth.minusMonths(RECURRING_MONTH_WINDOW - 1L).atDay(1);
		LocalDate to = referenceMonth.atEndOfMonth();
		List<Expense> expenses = expenseRepository.findAllByHouseholdIdAndDueDateGreaterThanEqual(householdId, from).stream()
			.filter(expense -> !expense.getOccurredOn().isAfter(to))
			.toList();

		return expenses.stream()
			.collect(Collectors.groupingBy(this::recurringKey, LinkedHashMap::new, Collectors.toList()))
			.values().stream()
			.map(this::toRecurringResponse)
			.filter(Objects::nonNull)
			.sorted(Comparator.comparing(RecurringExpenseResponse::occurrences).reversed().thenComparing(RecurringExpenseResponse::averageAmount).reversed())
			.toList();
	}

	@Transactional(readOnly = true)
	public BigDecimal totalByCategory(YearMonth referenceMonth, String categoryName) {
		return totalByCategory(accessContextProvider.requireContext(), referenceMonth, categoryName);
	}

	@Transactional(readOnly = true)
	BigDecimal totalByCategory(FinancialAssistantAccessContext context, YearMonth referenceMonth, String categoryName) {
		if (categoryName == null || categoryName.isBlank()) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		String normalizedCategory = FinancialAssistantSupport.normalizeText(categoryName);
		return summarize(context, referenceMonth.atDay(1), referenceMonth.atEndOfMonth()).categoryTotals().stream()
			.filter(category -> FinancialAssistantSupport.normalizeText(category.categoryName()).equals(normalizedCategory))
			.map(CategorySpendingResponse::totalAmount)
			.findFirst()
			.orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
	}

	@Transactional(readOnly = true)
	public CategorySpendingResponse highestSpendingCategory(YearMonth referenceMonth) {
		return highestSpendingCategory(accessContextProvider.requireContext(), referenceMonth);
	}

	@Transactional(readOnly = true)
	CategorySpendingResponse highestSpendingCategory(FinancialAssistantAccessContext context, YearMonth referenceMonth) {
		return summarize(context, referenceMonth.atDay(1), referenceMonth.atEndOfMonth()).categoryTotals().stream().findFirst().orElse(null);
	}

	@Transactional(readOnly = true)
	public List<TopExpenseResponse> topExpenses(YearMonth referenceMonth, int limit) {
		return topExpenses(accessContextProvider.requireContext(), referenceMonth, limit);
	}

	@Transactional(readOnly = true)
	List<TopExpenseResponse> topExpenses(FinancialAssistantAccessContext context, YearMonth referenceMonth, int limit) {
		PeriodSnapshot snapshot = snapshot(context, FinancialAssistantSupport.monthRange(referenceMonth));
		return topExpenses(snapshot.expenses(), limit);
	}

	private PeriodSnapshot snapshot(FinancialAssistantAccessContext context, FinancialAssistantDateRange range) {
		Long householdId = context.householdId();
		List<Expense> expenses = expenseRepository.findAllByHouseholdIdAndDueDateBetween(householdId, range.from(), range.to());
		Map<Long, List<Payment>> paymentsByExpenseId = paymentsByExpenseId(expenses);
		BigDecimal totalAmount = sumExpenseAmounts(expenses);
		BigDecimal paidAmount = expenses.stream()
			.map(expense -> sumPayments(paymentsByExpenseId.getOrDefault(expense.getId(), List.of())))
			.reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
		BigDecimal remainingAmount = totalAmount.subtract(paidAmount).max(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
		return new PeriodSnapshot(expenses, paymentsByExpenseId, totalAmount, paidAmount, remainingAmount);
	}

	private Map<Long, List<Payment>> paymentsByExpenseId(List<Expense> expenses) {
		List<Long> expenseIds = expenses.stream()
			.map(Expense::getId)
			.toList();
		if (expenseIds.isEmpty()) {
			return Map.of();
		}
		return paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(expenseIds).stream()
			.collect(Collectors.groupingBy(Payment::getExpenseId, LinkedHashMap::new, Collectors.toList()));
	}

	private BigDecimal sumExpenseAmounts(Collection<Expense> expenses) {
		return expenses.stream()
			.map(Expense::getAmount)
			.reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
	}

	private BigDecimal sumPayments(Collection<Payment> payments) {
		return payments.stream()
			.map(Payment::getAmount)
			.reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
	}

	private List<CategorySpendingResponse> categoryTotals(List<Expense> expenses) {
		BigDecimal totalAmount = sumExpenseAmounts(expenses);
		Map<Long, CategoryAccumulator> categories = new LinkedHashMap<>();
		for (Expense expense : expenses) {
			CategoryAccumulator accumulator = categories.computeIfAbsent(
				expense.getCategoryId(),
				key -> new CategoryAccumulator(expense.getCategoryId(), expense.getCategoryNameSnapshot())
			);
			accumulator.add(expense.getAmount());
		}
		return categories.values().stream()
			.map(accumulator -> accumulator.toResponse(totalAmount))
			.toList();
	}

	private List<TopExpenseResponse> topExpenses(List<Expense> expenses, int limit) {
		return expenses.stream()
			.sorted(Comparator.comparing(Expense::getAmount).reversed().thenComparing(Expense::getEffectiveDate).reversed().thenComparing(Expense::getId).reversed())
			.limit(Math.max(1, limit))
			.map(expense -> new TopExpenseResponse(
				expense.getId(),
				expense.getDescription(),
				expense.getAmount(),
				expense.getEffectiveDate(),
				expense.getCategoryNameSnapshot(),
				expense.getSubcategoryNameSnapshot(),
				expense.getContext()
			))
			.toList();
	}

	private String recurringKey(Expense expense) {
		return "%s|%s|%s".formatted(
			FinancialAssistantSupport.normalizeText(expense.getDescription()),
			FinancialAssistantSupport.normalizeText(expense.getCategoryNameSnapshot()),
			FinancialAssistantSupport.normalizeText(expense.getSubcategoryNameSnapshot())
		);
	}

	private RecurringExpenseResponse toRecurringResponse(List<Expense> expenses) {
		if (expenses.isEmpty()) {
			return null;
		}
		Map<YearMonth, List<Expense>> byMonth = expenses.stream()
			.collect(Collectors.groupingBy(expense -> YearMonth.from(expense.getOccurredOn()), LinkedHashMap::new, Collectors.toList()));
		if (byMonth.size() < 2) {
			return null;
		}
		List<BigDecimal> amounts = expenses.stream()
			.map(Expense::getAmount)
			.toList();
		BigDecimal averageAmount = amounts.stream()
			.reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
			.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
		BigDecimal min = amounts.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
		BigDecimal max = amounts.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
		BigDecimal tolerance = averageAmount.multiply(new BigDecimal("0.10")).max(new BigDecimal("20.00"));
		Expense latest = expenses.stream().max(Comparator.comparing(Expense::getOccurredOn).thenComparing(Expense::getId)).orElse(expenses.getFirst());
		return new RecurringExpenseResponse(
			latest.getDescription(),
			latest.getCategoryNameSnapshot(),
			latest.getSubcategoryNameSnapshot(),
			averageAmount,
			byMonth.size(),
			max.subtract(min).compareTo(tolerance) <= 0,
			latest.getOccurredOn()
		);
	}

	private static final class CategoryAccumulator {

		private final Long categoryId;
		private final String categoryName;
		private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		private long expensesCount;

		private CategoryAccumulator(Long categoryId, String categoryName) {
			this.categoryId = categoryId;
			this.categoryName = categoryName;
		}

		private void add(BigDecimal amount) {
			this.totalAmount = this.totalAmount.add(amount);
			this.expensesCount++;
		}

		private CategorySpendingResponse toResponse(BigDecimal overallTotal) {
			return new CategorySpendingResponse(
				categoryId,
				categoryName,
				totalAmount,
				expensesCount,
				FinancialAssistantSupport.percentage(totalAmount, overallTotal)
			);
		}
	}

	private record PeriodSnapshot(
		List<Expense> expenses,
		Map<Long, List<Payment>> paymentsByExpenseId,
		BigDecimal totalAmount,
		BigDecimal paidAmount,
		BigDecimal remainingAmount
	) {
	}
}
