package com.gilrossi.despesas.api.v1.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/platform")
public class PlatformAdminPlatformController {

	private final PlatformAdminPlatformService service;

	public PlatformAdminPlatformController(PlatformAdminPlatformService service) {
		this.service = service;
	}

	@GetMapping("/overview")
	public ApiResponse<PlatformAdminPlatformOverviewResponse> overview() {
		return new ApiResponse<>(service.readOverview());
	}

	@GetMapping("/health")
	public ApiResponse<PlatformAdminPlatformHealthResponse> health() {
		return new ApiResponse<>(service.readHealth());
	}
}
