package com.gilrossi.despesas.financialassistant;

import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialAssistantInsightsService {

	private final FinancialAssistantAnalyticsService analyticsService;
	private final FinancialAssistantAccessContextProvider accessContextProvider;

	public FinancialAssistantInsightsService(
		FinancialAssistantAnalyticsService analyticsService,
		FinancialAssistantAccessContextProvider accessContextProvider
	) {
		this.analyticsService = analyticsService;
		this.accessContextProvider = accessContextProvider;
	}

	@Transactional(readOnly = true)
	public FinancialAssistantInsightsResponse insights(YearMonth referenceMonth) {
		return insights(accessContextProvider.requireContext(), referenceMonth);
	}

	@Transactional(readOnly = true)
	FinancialAssistantInsightsResponse insights(FinancialAssistantAccessContext context, YearMonth referenceMonth) {
		return new FinancialAssistantInsightsResponse(
			analyticsService.compareMonths(context, referenceMonth),
			analyticsService.increaseAlerts(context, referenceMonth),
			analyticsService.recurringExpenses(context, referenceMonth)
		);
	}
}
