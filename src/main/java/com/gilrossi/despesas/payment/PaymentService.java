package com.gilrossi.despesas.payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gilrossi.despesas.api.v1.shared.PageInfo;
import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseNotFoundException;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.expense.ExpenseStatusCalculator;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class PaymentService {

	private final ExpenseRepository expenseRepository;
	private final PaymentRepository paymentRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;
	private final ExpenseStatusCalculator statusCalculator;

	public PaymentService(ExpenseRepository expenseRepository, PaymentRepository paymentRepository, CurrentHouseholdProvider currentHouseholdProvider, ExpenseStatusCalculator statusCalculator) {
		this.expenseRepository = expenseRepository;
		this.paymentRepository = paymentRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
		this.statusCalculator = statusCalculator;
	}

	@Transactional
	public PaymentResponse registrar(CreatePaymentRequest request) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Expense expense = expenseRepository.findByIdAndHouseholdIdForUpdate(request.expenseId(), householdId)
			.orElseThrow(() -> new ExpenseNotFoundException(request.expenseId()));
		List<Payment> existingPayments = paymentRepository.findByExpenseId(request.expenseId());
		BigDecimal alreadyPaid = sumAmounts(existingPayments);
		BigDecimal remaining = expense.getAmount().subtract(alreadyPaid);
		if (request.amount().compareTo(remaining) > 0) {
			throw new PaymentBusinessRuleException("Payment amount exceeds remaining expense balance");
		}

		Payment payment = new Payment(expense.getId(), request.amount(), request.paidAt(), request.method(), request.notes());
		Payment saved = paymentRepository.save(payment);
		BigDecimal paidAmount = alreadyPaid.add(saved.getAmount());
		BigDecimal remainingAmount = expense.getAmount().subtract(paidAmount).max(BigDecimal.ZERO);
		ExpenseStatus status = statusCalculator.calcular(expense.getAmount(), paidAmount, expense.getDueDate(), LocalDate.now());
		return new PaymentResponse(
			saved.getId(),
			expense.getId(),
			saved.getAmount(),
			saved.getPaidAt(),
			saved.getMethod(),
			saved.getNotes(),
			status,
			paidAmount,
			remainingAmount,
			saved.getCreatedAt(),
			saved.getUpdatedAt()
		);
	}

	@Transactional(readOnly = true)
	public PageResponse<PaymentResponse> listarPorDespesa(Long expenseId, int page, int size) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Expense expense = expenseRepository.findByIdAndHouseholdId(expenseId, householdId)
			.orElseThrow(() -> new ExpenseNotFoundException(expenseId));
		Page<Payment> payments = paymentRepository.findByExpenseIdOrderByPaidAtDescIdDesc(expenseId, PageRequest.of(Math.max(0, page), Math.max(1, size)));
		List<Payment> allPayments = paymentRepository.findByExpenseId(expenseId);
		BigDecimal paidAmount = allPayments.stream()
			.map(Payment::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal remainingAmount = expense.getAmount().subtract(paidAmount).max(BigDecimal.ZERO);
		ExpenseStatus status = statusCalculator.calcular(expense.getAmount(), paidAmount, expense.getDueDate(), LocalDate.now());
		List<PaymentResponse> content = payments.getContent().stream()
			.map(payment -> new PaymentResponse(
				payment.getId(),
				expense.getId(),
				payment.getAmount(),
				payment.getPaidAt(),
				payment.getMethod(),
				payment.getNotes(),
				status,
				paidAmount,
				remainingAmount,
				payment.getCreatedAt(),
				payment.getUpdatedAt()
			))
			.toList();
		return new PageResponse<>(
			content,
			new PageInfo(payments.getNumber(), payments.getSize(), payments.getTotalElements(), payments.getTotalPages(), payments.hasNext(), payments.hasPrevious())
		);
	}

	@Transactional
	public void deletar(Long paymentId) {
		Payment payment = paymentRepository.findActiveById(paymentId)
			.orElseThrow(() -> new PaymentNotFoundException(paymentId));
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		expenseRepository.findByIdAndHouseholdIdForUpdate(payment.getExpenseId(), householdId)
			.orElseThrow(() -> new ExpenseNotFoundException(payment.getExpenseId()));
		payment.markDeleted();
		paymentRepository.save(payment);
	}

	private BigDecimal sumAmounts(List<Payment> payments) {
		return payments.stream()
			.map(Payment::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
