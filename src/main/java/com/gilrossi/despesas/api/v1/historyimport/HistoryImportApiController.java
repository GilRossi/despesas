package com.gilrossi.despesas.api.v1.historyimport;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.historyimport.CreateHistoryImportRequest;
import com.gilrossi.despesas.historyimport.HistoryImportResponse;
import com.gilrossi.despesas.historyimport.HistoryImportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/history-imports")
public class HistoryImportApiController {

	private final HistoryImportService historyImportService;

	public HistoryImportApiController(HistoryImportService historyImportService) {
		this.historyImportService = historyImportService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<HistoryImportResponse>> importHistory(@Valid @RequestBody CreateHistoryImportRequest request) {
		return ResponseEntity.status(201).body(new ApiResponse<>(historyImportService.importHistory(request)));
	}
}
