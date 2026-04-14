package com.gilrossi.despesas.api.v1.driver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;

@RestController
@RequestMapping("/api/v1/driver")
public class DriverModuleProbeController {

	private final DriverModuleProbeService service;

	public DriverModuleProbeController(DriverModuleProbeService service) {
		this.service = service;
	}

	@GetMapping("/_probe")
	public ApiResponse<DriverModuleProbeResponse> probe() {
		return new ApiResponse<>(service.probe());
	}
}
