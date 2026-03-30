package com.gilrossi.despesas.api.v1.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gilrossi.despesas.api.v1.shared.ApiResponse;
import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.payment.CreatePaymentRequest;
import com.gilrossi.despesas.payment.PaymentResponse;
import com.gilrossi.despesas.payment.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentApiController {

	private final PaymentService paymentService;

	public PaymentApiController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@GetMapping("/expense/{expenseId}")
	public PageResponse<PaymentResponse> listarPorDespesa(
		@PathVariable Long expenseId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		return paymentService.listarPorDespesa(expenseId, page, size);
	}

	@PostMapping
	public ResponseEntity<ApiResponse<PaymentResponse>> registrar(@Valid @RequestBody CreatePaymentRequest request) {
		return ResponseEntity.status(201).body(new ApiResponse<>(this.paymentService.registrar(request)));
	}

	@DeleteMapping("/{paymentId}")
	public ResponseEntity<Void> deletar(@PathVariable Long paymentId) {
		paymentService.deletar(paymentId);
		return ResponseEntity.noContent().build();
	}
}
