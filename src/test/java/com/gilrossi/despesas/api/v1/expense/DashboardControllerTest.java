package com.gilrossi.despesas.api.v1.expense;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.expense.DashboardStatusSummary;
import com.gilrossi.despesas.expense.DashboardSummaryResponse;
import com.gilrossi.despesas.expense.DashboardSummaryService;
import com.gilrossi.despesas.expense.ExpenseStatus;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class DashboardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DashboardSummaryService dashboardSummaryService;

	@Test
	void deve_retornar_resumo_para_dashboard_flutter() throws Exception {
		when(dashboardSummaryService.resumir()).thenReturn(new DashboardSummaryResponse(
			7L,
			3,
			new BigDecimal("470.00"),
			new BigDecimal("40.00"),
			new BigDecimal("430.00"),
			1,
			new BigDecimal("120.00"),
			2,
			new BigDecimal("350.00"),
			List.of(
				new DashboardStatusSummary(ExpenseStatus.VENCIDA, 1, new BigDecimal("120.00")),
				new DashboardStatusSummary(ExpenseStatus.PARCIALMENTE_PAGA, 1, new BigDecimal("150.00"))
			)
		));

		mockMvc.perform(get("/api/v1/dashboard/summary"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.householdId").value(7))
			.andExpect(jsonPath("$.data.totalExpenses").value(3))
			.andExpect(jsonPath("$.data.statuses[0].status").value("VENCIDA"));
	}
}
