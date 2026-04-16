package com.gilrossi.despesas.api.v1.driver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;

@RestController
@RequestMapping("/api/v1/driver")
public class DriverModuleBootstrapController {

	private final DriverModuleBootstrapService service;

	public DriverModuleBootstrapController(DriverModuleBootstrapService service) {
		this.service = service;
	}

	@GetMapping("/bootstrap")
	public ApiResponse<DriverModuleBootstrapResponse> bootstrap() {
		return new ApiResponse<>(service.bootstrap());
	}
}
