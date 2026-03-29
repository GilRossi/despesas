package com.gilrossi.despesas.api.v1.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.dashboard.UserDashboardResponse;
import com.gilrossi.despesas.dashboard.UserDashboardService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class UserDashboardController {

	private final UserDashboardService userDashboardService;

	public UserDashboardController(UserDashboardService userDashboardService) {
		this.userDashboardService = userDashboardService;
	}

	@GetMapping
	public ApiResponse<UserDashboardResponse> read() {
		return new ApiResponse<>(userDashboardService.read());
	}
}
