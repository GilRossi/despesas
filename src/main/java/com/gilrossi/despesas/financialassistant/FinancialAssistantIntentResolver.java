package com.gilrossi.despesas.financialassistant;

import java.time.YearMonth;
import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Component
public class FinancialAssistantIntentResolver {

	private final CurrentHouseholdProvider currentHouseholdProvider;
	private final CategoryRepository categoryRepository;

	public FinancialAssistantIntentResolver(CurrentHouseholdProvider currentHouseholdProvider, CategoryRepository categoryRepository) {
		this.currentHouseholdProvider = currentHouseholdProvider;
		this.categoryRepository = categoryRepository;
	}

	public ResolvedFinancialAssistantQuery resolve(String question, YearMonth defaultReferenceMonth) {
		String normalizedQuestion = FinancialAssistantSupport.normalizeText(question);
		String categoryName = resolveCategoryName(normalizedQuestion);

		if (normalizedQuestion.contains("economiz")) {
			return resolvedQuery(FinancialAssistantIntent.SAVINGS_RECOMMENDATIONS, normalizedQuestion, defaultReferenceMonth);
		}
		if (normalizedQuestion.contains("recorrent") || normalizedQuestion.contains("todo mes") || normalizedQuestion.contains("todo mes") || normalizedQuestion.contains("fix")) {
			return resolvedQuery(FinancialAssistantIntent.RECURRING_EXPENSES, normalizedQuestion, defaultReferenceMonth);
		}
		if (normalizedQuestion.contains("aument") || normalizedQuestion.contains("subiu")) {
			return resolvedQuery(FinancialAssistantIntent.INCREASE_ALERTS, normalizedQuestion, defaultReferenceMonth);
		}
		if (categoryName != null && (normalizedQuestion.contains("quanto") || normalizedQuestion.contains("gastei") || normalizedQuestion.contains("total"))) {
			return resolvedQuery(FinancialAssistantIntent.TOTAL_BY_CATEGORY_IN_PERIOD, normalizedQuestion, defaultReferenceMonth, categoryName);
		}
		if (normalizedQuestion.contains("mes passado") || normalizedQuestion.contains("compar") || normalizedQuestion.contains("relacao ao mes passado")) {
			return resolvedQuery(FinancialAssistantIntent.MONTH_OVER_MONTH_CHANGE, normalizedQuestion, defaultReferenceMonth);
		}
		if (normalizedQuestion.contains("maiores gastos") || normalizedQuestion.contains("maiores despesas") || normalizedQuestion.contains("top gastos") || normalizedQuestion.contains("top despesas")) {
			return resolvedQuery(FinancialAssistantIntent.TOP_EXPENSES_IN_PERIOD, normalizedQuestion, defaultReferenceMonth);
		}
		if (normalizedQuestion.contains("onde estou gastando mais") || normalizedQuestion.contains("gasto mais") || normalizedQuestion.contains("categoria que mais")) {
			return resolvedQuery(FinancialAssistantIntent.HIGHEST_SPENDING_CATEGORY, normalizedQuestion, defaultReferenceMonth);
		}
		if (normalizedQuestion.contains("resumo") || normalizedQuestion.contains("este mes") || normalizedQuestion.contains("esse mes") || normalizedQuestion.contains("como estou")) {
			return resolvedQuery(FinancialAssistantIntent.PERIOD_SUMMARY, normalizedQuestion, defaultReferenceMonth);
		}
		return resolvedQuery(FinancialAssistantIntent.UNKNOWN, normalizedQuestion, defaultReferenceMonth, categoryName);
	}

	private ResolvedFinancialAssistantQuery resolvedQuery(
		FinancialAssistantIntent intent,
		String normalizedQuestion,
		YearMonth defaultReferenceMonth
	) {
		return resolvedQuery(intent, normalizedQuestion, defaultReferenceMonth, resolveCategoryName(normalizedQuestion));
	}

	private ResolvedFinancialAssistantQuery resolvedQuery(
		FinancialAssistantIntent intent,
		String normalizedQuestion,
		YearMonth defaultReferenceMonth,
		String categoryName
	) {
		return new ResolvedFinancialAssistantQuery(intent, resolveReferenceMonth(intent, normalizedQuestion, defaultReferenceMonth), categoryName);
	}

	private YearMonth resolveReferenceMonth(FinancialAssistantIntent intent, String normalizedQuestion, YearMonth defaultReferenceMonth) {
		if (normalizedQuestion.contains("mes passado") && !isComparativeIntent(intent)) {
			return defaultReferenceMonth.minusMonths(1);
		}
		return defaultReferenceMonth;
	}

	private boolean isComparativeIntent(FinancialAssistantIntent intent) {
		return intent == FinancialAssistantIntent.INCREASE_ALERTS || intent == FinancialAssistantIntent.MONTH_OVER_MONTH_CHANGE;
	}

	private String resolveCategoryName(String normalizedQuestion) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		return categoryRepository.findActiveByHouseholdId(householdId).stream()
			.map(Category::getName)
			.filter(name -> normalizedQuestion.contains(FinancialAssistantSupport.normalizeText(name)))
			.max(Comparator.comparingInt(String::length))
			.orElse(null);
	}
}
