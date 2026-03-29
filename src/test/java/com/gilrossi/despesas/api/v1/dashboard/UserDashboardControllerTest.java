package com.gilrossi.despesas.api.v1.dashboard;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gilrossi.despesas.api.v1.shared.ApiExceptionHandler;
import com.gilrossi.despesas.dashboard.UserDashboardResponse;
import com.gilrossi.despesas.dashboard.UserDashboardService;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.identity.HouseholdMemberRole;
import com.gilrossi.despesas.spacereference.SpaceReferenceTypeGroup;

@WebMvcTest(UserDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class UserDashboardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserDashboardService userDashboardService;

	@Test
	void deve_retornar_dashboard_role_aware() throws Exception {
		when(userDashboardService.read()).thenReturn(new UserDashboardResponse(
			HouseholdMemberRole.OWNER,
			new UserDashboardResponse.SummaryMain(
				"2026-03",
				new BigDecimal("400.00"),
				new BigDecimal("120.00"),
				new BigDecimal("80.00"),
				3,
				1
			),
			new UserDashboardResponse.ActionNeeded(
				1,
				new BigDecimal("120.00"),
				3,
				new BigDecimal("400.00"),
				List.of(new UserDashboardResponse.ActionItem(
					9L,
					"Internet",
					LocalDate.of(2026, 3, 10),
					ExpenseStatus.VENCIDA,
					new BigDecimal("120.00"),
					"/expenses"
				))
			),
			new UserDashboardResponse.RecentActivity(List.of(
				new UserDashboardResponse.RecentActivityItem(
					"PAYMENT_RECORDED",
					"Pagamento registrado",
					"Internet",
					new BigDecimal("80.00"),
					Instant.parse("2026-03-29T10:15:30Z"),
					"/expenses"
				)
			)),
			new UserDashboardResponse.AssistantCard(
				"Assistente financeiro",
				"Revise a categoria que mais pesa",
				"OPEN_ASSISTANT",
				"/assistant"
			),
			new UserDashboardResponse.MonthOverview(
				"2026-03",
				new BigDecimal("900.00"),
				new BigDecimal("500.00"),
				new BigDecimal("400.00"),
				new UserDashboardResponse.MonthComparison(
					"2026-03",
					new BigDecimal("900.00"),
					"2026-02",
					new BigDecimal("700.00"),
					new BigDecimal("200.00"),
					new BigDecimal("28.57")
				),
				new UserDashboardResponse.HighestSpendingCategory(
					3L,
					"Moradia",
					new BigDecimal("450.00"),
					new BigDecimal("50.00")
				)
			),
			new UserDashboardResponse.CategorySpending(List.of(
				new UserDashboardResponse.CategorySpendingItem(
					3L,
					"Moradia",
					new BigDecimal("450.00"),
					2,
					new BigDecimal("50.00")
				)
			)),
			new UserDashboardResponse.HouseholdSummary(
				2,
				1,
				1,
				3,
				List.of(new UserDashboardResponse.ReferenceGroupSummary(
					SpaceReferenceTypeGroup.RESIDENCIAL,
					2
				))
			),
			null
		));

		mockMvc.perform(get("/api/v1/dashboard"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.role").value("OWNER"))
			.andExpect(jsonPath("$.data.summaryMain.referenceMonth").value("2026-03"))
			.andExpect(jsonPath("$.data.actionNeeded.items[0].status").value("VENCIDA"))
			.andExpect(jsonPath("$.data.recentActivity.items[0].type").value("PAYMENT_RECORDED"))
			.andExpect(jsonPath("$.data.monthOverview.referenceMonth").value("2026-03"))
			.andExpect(jsonPath("$.data.categorySpending.items[0].categoryName").value("Moradia"))
			.andExpect(jsonPath("$.data.householdSummary.referencesByGroup[0].group").value("RESIDENCIAL"))
			.andExpect(jsonPath("$.data.quickActions").doesNotExist());
	}
}
