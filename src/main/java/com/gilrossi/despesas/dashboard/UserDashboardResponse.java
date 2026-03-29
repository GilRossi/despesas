package com.gilrossi.despesas.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.spacereference.SpaceReferenceTypeGroup;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDashboardResponse(
	HouseholdMemberRole role,
	SummaryMain summaryMain,
	ActionNeeded actionNeeded,
	RecentActivity recentActivity,
	AssistantCard assistantCard,
	MonthOverview monthOverview,
	CategorySpending categorySpending,
	HouseholdSummary householdSummary,
	QuickActions quickActions
) {

	public record SummaryMain(
		String referenceMonth,
		BigDecimal totalOpenAmount,
		BigDecimal totalOverdueAmount,
		BigDecimal paidThisMonthAmount,
		long openCount,
		long overdueCount
	) {
	}

	public record ActionNeeded(
		long overdueCount,
		BigDecimal overdueAmount,
		long openCount,
		BigDecimal openAmount,
		List<ActionItem> items
	) {
	}

	public record ActionItem(
		Long expenseId,
		String description,
		LocalDate dueDate,
		ExpenseStatus status,
		BigDecimal amount,
		String route
	) {
	}

	public record RecentActivity(List<RecentActivityItem> items) {
	}

	public record RecentActivityItem(
		String type,
		String title,
		String subtitle,
		BigDecimal amount,
		Instant occurredAt,
		String route
	) {
	}

	public record AssistantCard(
		String title,
		String message,
		String primaryActionKey,
		String route
	) {
	}

	public record MonthOverview(
		String referenceMonth,
		BigDecimal totalAmount,
		BigDecimal paidAmount,
		BigDecimal remainingAmount,
		MonthComparison monthComparison,
		HighestSpendingCategory highestSpendingCategory
	) {
	}

	public record MonthComparison(
		String currentMonth,
		BigDecimal currentTotal,
		String previousMonth,
		BigDecimal previousTotal,
		BigDecimal deltaAmount,
		BigDecimal deltaPercentage
	) {
	}

	public record HighestSpendingCategory(
		Long categoryId,
		String categoryName,
		BigDecimal totalAmount,
		BigDecimal sharePercentage
	) {
	}

	public record CategorySpending(List<CategorySpendingItem> items) {
	}

	public record CategorySpendingItem(
		Long categoryId,
		String categoryName,
		BigDecimal totalAmount,
		long expensesCount,
		BigDecimal sharePercentage
	) {
	}

	public record HouseholdSummary(
		int membersCount,
		int ownersCount,
		int membersOnlyCount,
		int spaceReferencesCount,
		List<ReferenceGroupSummary> referencesByGroup
	) {
	}

	public record ReferenceGroupSummary(
		SpaceReferenceTypeGroup group,
		int count
	) {
	}

	public record QuickActions(List<QuickActionItem> items) {
	}

	public record QuickActionItem(
		String key,
		String label,
		String route
	) {
	}
}
