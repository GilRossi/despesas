package com.gilrossi.despesas.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.api.v1.space.SpaceReferenceResponse;
import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.expense.ExpenseStatusCalculator;
import com.gilrossi.despesas.financialassistant.CategorySpendingResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAccessContext;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAccessContextProvider;
import com.gilrossi.despesas.financialassistant.FinancialAssistantAnalyticsService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantPeriodSummaryResponse;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationService;
import com.gilrossi.despesas.financialassistant.FinancialAssistantRecommendationsResponse;
import com.gilrossi.despesas.financialassistant.MonthComparisonResponse;
import com.gilrossi.despesas.financialassistant.RecommendationResponse;
import com.gilrossi.despesas.identity.HouseholdMemberResponse;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.identity.HouseholdMemberService;
import com.gilrossi.despesas.payment.Payment;
import com.gilrossi.despesas.payment.PaymentRepository;
import com.gilrossi.despesas.spacereference.SpaceReferenceService;

@Service
public class UserDashboardService {

	private static final String DEFAULT_ROUTE = "/expenses";
	private static final String ASSISTANT_ROUTE = "/assistant";
	private static final String REPORTS_ROUTE = "/reports";

	private final ExpenseRepository expenseRepository;
	private final PaymentRepository paymentRepository;
	private final ExpenseStatusCalculator expenseStatusCalculator;
	private final FinancialAssistantAccessContextProvider accessContextProvider;
	private final FinancialAssistantAnalyticsService analyticsService;
	private final FinancialAssistantRecommendationService recommendationService;
	private final HouseholdMemberService householdMemberService;
	private final SpaceReferenceService spaceReferenceService;

	public UserDashboardService(
		ExpenseRepository expenseRepository,
		PaymentRepository paymentRepository,
		ExpenseStatusCalculator expenseStatusCalculator,
		FinancialAssistantAccessContextProvider accessContextProvider,
		FinancialAssistantAnalyticsService analyticsService,
		FinancialAssistantRecommendationService recommendationService,
		HouseholdMemberService householdMemberService,
		SpaceReferenceService spaceReferenceService
	) {
		this.expenseRepository = expenseRepository;
		this.paymentRepository = paymentRepository;
		this.expenseStatusCalculator = expenseStatusCalculator;
		this.accessContextProvider = accessContextProvider;
		this.analyticsService = analyticsService;
		this.recommendationService = recommendationService;
		this.householdMemberService = householdMemberService;
		this.spaceReferenceService = spaceReferenceService;
	}

	@Transactional(readOnly = true)
	public UserDashboardResponse read() {
		FinancialAssistantAccessContext context = accessContextProvider.requireContext();
		HouseholdMemberRole role = HouseholdMemberRole.valueOf(context.role());
		YearMonth referenceMonth = YearMonth.now();

		List<Expense> expenses = expenseRepository.findAllByHouseholdId(context.householdId());
		Map<Long, List<Payment>> paymentsByExpenseId = paymentsByExpenseId(expenses);
		List<ExpenseSnapshot> snapshots = expenses.stream()
			.map(expense -> snapshot(expense, paymentsByExpenseId.getOrDefault(expense.getId(), List.of())))
			.toList();

		FinancialAssistantPeriodSummaryResponse monthSummary = analyticsService.summarize(referenceMonth.atDay(1), referenceMonth.atEndOfMonth());
		FinancialAssistantRecommendationsResponse recommendations = recommendationService.recommendations(referenceMonth);

		UserDashboardResponse.SummaryMain summaryMain = buildSummaryMain(referenceMonth, snapshots, monthSummary);
		UserDashboardResponse.ActionNeeded actionNeeded = buildActionNeeded(snapshots);
		UserDashboardResponse.RecentActivity recentActivity = buildRecentActivity(expenses, paymentsByExpenseId);
		UserDashboardResponse.AssistantCard assistantCard = buildAssistantCard(recommendations);

		if (role == HouseholdMemberRole.OWNER) {
			return new UserDashboardResponse(
				role,
				summaryMain,
				actionNeeded,
				recentActivity,
				assistantCard,
				buildMonthOverview(referenceMonth, monthSummary),
				buildCategorySpending(monthSummary),
				buildHouseholdSummary(),
				null
			);
		}

		return new UserDashboardResponse(
			role,
			summaryMain,
			actionNeeded,
			recentActivity,
			assistantCard,
			null,
			null,
			null,
			buildQuickActions()
		);
	}

	private UserDashboardResponse.SummaryMain buildSummaryMain(
		YearMonth referenceMonth,
		List<ExpenseSnapshot> snapshots,
		FinancialAssistantPeriodSummaryResponse monthSummary
	) {
		long openCount = snapshots.stream().filter(snapshot -> isOpen(snapshot.status())).count();
		BigDecimal totalOpenAmount = snapshots.stream()
			.filter(snapshot -> isOpen(snapshot.status()))
			.map(ExpenseSnapshot::remainingAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		long overdueCount = snapshots.stream().filter(snapshot -> snapshot.status() == ExpenseStatus.VENCIDA).count();
		BigDecimal totalOverdueAmount = snapshots.stream()
			.filter(snapshot -> snapshot.status() == ExpenseStatus.VENCIDA)
			.map(ExpenseSnapshot::remainingAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new UserDashboardResponse.SummaryMain(
			referenceMonth.toString(),
			totalOpenAmount,
			totalOverdueAmount,
			monthSummary.paidAmount(),
			openCount,
			overdueCount
		);
	}

	private UserDashboardResponse.ActionNeeded buildActionNeeded(List<ExpenseSnapshot> snapshots) {
		List<ExpenseSnapshot> prioritized = snapshots.stream()
			.filter(snapshot -> snapshot.status() == ExpenseStatus.VENCIDA || isOpen(snapshot.status()))
			.sorted(Comparator
				.comparing((ExpenseSnapshot snapshot) -> priority(snapshot.status()))
				.thenComparing(snapshot -> snapshot.expense().getDueDate())
				.thenComparing(snapshot -> snapshot.expense().getId()))
			.toList();
		long overdueCount = prioritized.stream().filter(snapshot -> snapshot.status() == ExpenseStatus.VENCIDA).count();
		BigDecimal overdueAmount = prioritized.stream()
			.filter(snapshot -> snapshot.status() == ExpenseStatus.VENCIDA)
			.map(ExpenseSnapshot::remainingAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		long openCount = prioritized.stream().filter(snapshot -> isOpen(snapshot.status())).count();
		BigDecimal openAmount = prioritized.stream()
			.filter(snapshot -> isOpen(snapshot.status()))
			.map(ExpenseSnapshot::remainingAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);

		List<UserDashboardResponse.ActionItem> items = prioritized.stream()
			.limit(3)
			.map(snapshot -> new UserDashboardResponse.ActionItem(
				snapshot.expense().getId(),
				snapshot.expense().getDescription(),
				snapshot.expense().getDueDate(),
				snapshot.status(),
				snapshot.remainingAmount(),
				DEFAULT_ROUTE
			))
			.toList();

		return new UserDashboardResponse.ActionNeeded(
			overdueCount,
			overdueAmount,
			openCount,
			openAmount,
			items
		);
	}

	private UserDashboardResponse.RecentActivity buildRecentActivity(
		List<Expense> expenses,
		Map<Long, List<Payment>> paymentsByExpenseId
	) {
		Map<Long, Expense> expensesById = expenses.stream()
			.collect(Collectors.toMap(Expense::getId, expense -> expense, (left, right) -> left, LinkedHashMap::new));
		List<UserDashboardResponse.RecentActivityItem> items = new ArrayList<>();

		for (Expense expense : expenses) {
			items.add(new UserDashboardResponse.RecentActivityItem(
				"EXPENSE_CREATED",
				"Despesa adicionada",
				expense.getDescription(),
				expense.getAmount(),
				expense.getCreatedAt(),
				DEFAULT_ROUTE
			));
		}
		for (List<Payment> payments : paymentsByExpenseId.values()) {
			for (Payment payment : payments) {
				Expense expense = expensesById.get(payment.getExpenseId());
				items.add(new UserDashboardResponse.RecentActivityItem(
					"PAYMENT_RECORDED",
					"Pagamento registrado",
					expense == null ? "Pagamento vinculado a despesa" : expense.getDescription(),
					payment.getAmount(),
					payment.getCreatedAt(),
					DEFAULT_ROUTE
				));
			}
		}

		return new UserDashboardResponse.RecentActivity(items.stream()
			.sorted(Comparator.comparing(UserDashboardResponse.RecentActivityItem::occurredAt).reversed())
			.limit(5)
			.toList());
	}

	private UserDashboardResponse.AssistantCard buildAssistantCard(FinancialAssistantRecommendationsResponse recommendations) {
		RecommendationResponse recommendation = recommendations.recommendations().isEmpty()
			? null
			: recommendations.recommendations().getFirst();
		String message = recommendation == null
			? "Veja um resumo rapido do seu mes e abra o assistente para aprofundar."
			: recommendation.rationale();
		return new UserDashboardResponse.AssistantCard(
			"Assistente financeiro",
			message,
			"OPEN_ASSISTANT",
			ASSISTANT_ROUTE
		);
	}

	private UserDashboardResponse.MonthOverview buildMonthOverview(
		YearMonth referenceMonth,
		FinancialAssistantPeriodSummaryResponse monthSummary
	) {
		MonthComparisonResponse comparison = analyticsService.compareMonths(referenceMonth);
		CategorySpendingResponse highestCategory = monthSummary.categoryTotals().isEmpty()
			? null
			: monthSummary.categoryTotals().getFirst();
		return new UserDashboardResponse.MonthOverview(
			referenceMonth.toString(),
			monthSummary.totalAmount(),
			monthSummary.paidAmount(),
			monthSummary.remainingAmount(),
			new UserDashboardResponse.MonthComparison(
				comparison.currentMonth(),
				comparison.currentTotal(),
				comparison.previousMonth(),
				comparison.previousTotal(),
				comparison.deltaAmount(),
				comparison.deltaPercentage()
			),
			highestCategory == null ? null : new UserDashboardResponse.HighestSpendingCategory(
				highestCategory.categoryId(),
				highestCategory.categoryName(),
				highestCategory.totalAmount(),
				highestCategory.sharePercentage()
			)
		);
	}

	private UserDashboardResponse.CategorySpending buildCategorySpending(FinancialAssistantPeriodSummaryResponse monthSummary) {
		return new UserDashboardResponse.CategorySpending(monthSummary.categoryTotals().stream()
			.map(category -> new UserDashboardResponse.CategorySpendingItem(
				category.categoryId(),
				category.categoryName(),
				category.totalAmount(),
				category.expensesCount(),
				category.sharePercentage()
			))
			.toList());
	}

	private UserDashboardResponse.HouseholdSummary buildHouseholdSummary() {
		List<HouseholdMemberResponse> members = householdMemberService.listMembers();
		List<SpaceReferenceResponse> references = spaceReferenceService.list(null, null, null).stream()
			.map(SpaceReferenceResponse::from)
			.toList();
		int ownersCount = (int) members.stream().filter(member -> member.role() == HouseholdMemberRole.OWNER).count();
		int membersOnlyCount = (int) members.stream().filter(member -> member.role() == HouseholdMemberRole.MEMBER).count();
		List<UserDashboardResponse.ReferenceGroupSummary> referencesByGroup = references.stream()
			.collect(Collectors.groupingBy(SpaceReferenceResponse::typeGroup, LinkedHashMap::new, Collectors.counting()))
			.entrySet().stream()
			.sorted(Map.Entry.comparingByKey(Comparator.comparing(Enum::name)))
			.map(entry -> new UserDashboardResponse.ReferenceGroupSummary(entry.getKey(), entry.getValue().intValue()))
			.toList();

		return new UserDashboardResponse.HouseholdSummary(
			members.size(),
			ownersCount,
			membersOnlyCount,
			references.size(),
			referencesByGroup
		);
	}

	private UserDashboardResponse.QuickActions buildQuickActions() {
		return new UserDashboardResponse.QuickActions(List.of(
			new UserDashboardResponse.QuickActionItem("OPEN_EXPENSES", "Ver despesas", DEFAULT_ROUTE),
			new UserDashboardResponse.QuickActionItem("OPEN_ASSISTANT", "Abrir assistente", ASSISTANT_ROUTE),
			new UserDashboardResponse.QuickActionItem("OPEN_REPORTS", "Ver relatorios", REPORTS_ROUTE)
		));
	}

	private Map<Long, List<Payment>> paymentsByExpenseId(List<Expense> expenses) {
		List<Long> expenseIds = expenses.stream().map(Expense::getId).toList();
		if (expenseIds.isEmpty()) {
			return Map.of();
		}
		return paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(expenseIds).stream()
			.collect(Collectors.groupingBy(Payment::getExpenseId, LinkedHashMap::new, Collectors.toList()));
	}

	private ExpenseSnapshot snapshot(Expense expense, List<Payment> payments) {
		BigDecimal paidAmount = sumPayments(payments);
		BigDecimal remainingAmount = expense.getAmount().subtract(paidAmount).max(BigDecimal.ZERO);
		ExpenseStatus status = expenseStatusCalculator.calcular(
			expense.getAmount(),
			paidAmount,
			expense.getDueDate(),
			LocalDate.now()
		);
		return new ExpenseSnapshot(expense, status, remainingAmount);
	}

	private BigDecimal sumPayments(Collection<Payment> payments) {
		return payments.stream()
			.map(Payment::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private boolean isOpen(ExpenseStatus status) {
		return status == ExpenseStatus.PREVISTA || status == ExpenseStatus.ABERTA || status == ExpenseStatus.PARCIALMENTE_PAGA;
	}

	private int priority(ExpenseStatus status) {
		return switch (status) {
			case VENCIDA -> 0;
			case ABERTA -> 1;
			case PARCIALMENTE_PAGA -> 2;
			case PREVISTA -> 3;
			default -> 4;
		};
	}

	private record ExpenseSnapshot(
		Expense expense,
		ExpenseStatus status,
		BigDecimal remainingAmount
	) {
	}
}
