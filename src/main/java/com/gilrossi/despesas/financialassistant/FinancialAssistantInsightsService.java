package com.gilrossi.despesas.financialassistant;

import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialAssistantInsightsService {

	private final FinancialAssistantAnalyticsService analyticsService;

	public FinancialAssistantInsightsService(FinancialAssistantAnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@Transactional(readOnly = true)
	public FinancialAssistantInsightsResponse insights(YearMonth referenceMonth) {
		return new FinancialAssistantInsightsResponse(
			analyticsService.compareMonths(referenceMonth),
			analyticsService.increaseAlerts(referenceMonth),
			analyticsService.recurringExpenses(referenceMonth)
		);
	}
}
