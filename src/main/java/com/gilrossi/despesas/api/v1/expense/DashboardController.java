package com.gilrossi.despesas.api.v1.expense;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.expense.DashboardSummaryResponse;
import com.gilrossi.despesas.expense.DashboardSummaryService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

	private final DashboardSummaryService dashboardSummaryService;

	public DashboardController(DashboardSummaryService dashboardSummaryService) {
		this.dashboardSummaryService = dashboardSummaryService;
	}

	@GetMapping("/summary")
	public ApiResponse<DashboardSummaryResponse> resumo() {
		return new ApiResponse<>(dashboardSummaryService.resumir());
	}
}
