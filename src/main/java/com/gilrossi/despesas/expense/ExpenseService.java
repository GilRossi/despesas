package com.gilrossi.despesas.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.api.v1.shared.PageInfo;
import com.gilrossi.despesas.api.v1.shared.PageResponse;
import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryNotFoundException;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryNotFoundException;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.payment.Payment;
import com.gilrossi.despesas.payment.PaymentRepository;
import com.gilrossi.despesas.payment.PaymentResponse;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class ExpenseService {

	private static final int MAX_PAGE_SIZE = 50;
	private static final Sort DEFAULT_LIST_SORT = Sort.by(Sort.Order.desc("dueDate"), Sort.Order.desc("id"));

	private final ExpenseRepository expenseRepository;
	private final PaymentRepository paymentRepository;
	private final CategoryRepository categoryRepository;
	private final SubcategoryRepository subcategoryRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;
	private final ExpenseStatusCalculator statusCalculator;

	public ExpenseService(
		ExpenseRepository expenseRepository,
		PaymentRepository paymentRepository,
		CategoryRepository categoryRepository,
		SubcategoryRepository subcategoryRepository,
		CurrentHouseholdProvider currentHouseholdProvider,
		ExpenseStatusCalculator statusCalculator
	) {
		this.expenseRepository = expenseRepository;
		this.paymentRepository = paymentRepository;
		this.categoryRepository = categoryRepository;
		this.subcategoryRepository = subcategoryRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
		this.statusCalculator = statusCalculator;
	}

	@Transactional(readOnly = true)
	public PageResponse<ExpenseResponse> listar(ExpenseFilter filter, int page, int size) {
		return listar(
			new ExpenseQuery(
				filter == null ? null : filter.q(),
				filter == null ? null : filter.context(),
				filter == null ? null : filter.categoryId(),
				filter == null ? null : filter.subcategoryId(),
				filter == null ? null : filter.status(),
				filter == null ? null : filter.overdue(),
				filter == null ? null : filter.dueDateFrom(),
				filter == null ? null : filter.dueDateTo(),
				filter == null ? null : filter.hasPayments()
			),
			page,
			size
		);
	}

	@Transactional(readOnly = true)
	public PageResponse<ExpenseResponse> listar(ExpenseQuery query, int page, int size) {
		PageRequest pageable = paginaRequest(page, size);
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Page<Expense> expenses = expenseRepository.findAllByFilters(
			householdId,
			normalizeOptional(query.q()),
			query.context(),
			query.categoryId(),
			query.subcategoryId(),
			query.status(),
			query.overdue(),
			query.dueDateFrom(),
			query.dueDateTo(),
			query.hasPayments(),
			pageable
		);
		Map<Long, List<Payment>> paymentsByExpenseId = carregarPagamentos(expenses.getContent());
		List<ExpenseResponse> content = expenses.stream()
			.map(expense -> toResponse(expense, paymentsByExpenseId.getOrDefault(expense.getId(), List.of())))
			.toList();
		return new PageResponse<>(
			content,
			new PageInfo(
				expenses.getNumber(),
				expenses.getSize(),
				expenses.getTotalElements(),
				expenses.getTotalPages(),
				expenses.hasNext(),
				expenses.hasPrevious()
			)
		);
	}

	@Transactional
	public ExpenseResponse criar(CreateExpenseRequest request) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Category category = requireCategory(householdId, request.categoryId());
		Subcategory subcategory = requireSubcategory(householdId, request.subcategoryId(), category.getId());
		Expense expense = new Expense(
			householdId,
			normalizeRequired(request.description(), "description"),
			requirePositive(request.amount(), "amount"),
			requireDate(request.dueDate()),
			requireContext(request.context()),
			category.getId(),
			category.getName(),
			subcategory.getId(),
			subcategory.getName(),
			normalizeOptional(request.notes())
		);
		Expense saved = expenseRepository.save(expense);
		return toResponse(saved, paymentRepository.findByExpenseId(saved.getId()));
	}

	@Transactional(readOnly = true)
	public ExpenseDetailResponse detalhar(Long id) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Expense expense = expenseRepository.findByIdAndHouseholdId(id, householdId)
			.orElseThrow(() -> new ExpenseNotFoundException(id));
		return toDetailResponse(expense, paymentRepository.findByExpenseId(expense.getId()));
	}

	@Transactional
	public ExpenseDetailResponse atualizar(Long id, UpdateExpenseRequest request) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Expense expense = expenseRepository.findByIdAndHouseholdIdForUpdate(id, householdId)
			.orElseThrow(() -> new ExpenseNotFoundException(id));
		Category category = requireCategory(householdId, request.categoryId());
		Subcategory subcategory = requireSubcategory(householdId, request.subcategoryId(), category.getId());
		List<Payment> existingPayments = paymentRepository.findByExpenseId(expense.getId());
		BigDecimal updatedAmount = requirePositive(request.amount(), "amount");
		validateAmountAgainstPaidAmount(updatedAmount, existingPayments);
		expense.setDescription(normalizeRequired(request.description(), "description"));
		expense.setAmount(updatedAmount);
		expense.setDueDate(requireDate(request.dueDate()));
		expense.setContext(requireContext(request.context()));
		expense.setCategoryId(category.getId());
		expense.setCategoryNameSnapshot(category.getName());
		expense.setSubcategoryId(subcategory.getId());
		expense.setSubcategoryNameSnapshot(subcategory.getName());
		expense.setNotes(normalizeOptional(request.notes()));
		expense.touch();
		Expense saved = expenseRepository.save(expense);
		return toDetailResponse(saved, existingPayments);
	}

	@Transactional
	public void deletar(Long id) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Expense expense = expenseRepository.findByIdAndHouseholdIdForUpdate(id, householdId)
			.orElseThrow(() -> new ExpenseNotFoundException(id));
		List<Payment> existingPayments = paymentRepository.findByExpenseId(expense.getId());
		if (!existingPayments.isEmpty()) {
			throw new IllegalArgumentException("Expense with payments cannot be deleted");
		}
		expense.markDeleted();
		expenseRepository.save(expense);
	}

	private Map<Long, List<Payment>> carregarPagamentos(List<Expense> expenses) {
		List<Long> expenseIds = expenses.stream()
			.map(Expense::getId)
			.toList();
		if (expenseIds.isEmpty()) {
			return Map.of();
		}
		return paymentRepository.findAllByExpenseIdInOrderByExpenseIdAscPaidAtDescIdDesc(expenseIds).stream()
			.collect(Collectors.groupingBy(Payment::getExpenseId, LinkedHashMap::new, Collectors.toList()));
	}

	private PageRequest paginaRequest(int page, int size) {
		int paginaNormalizada = Math.max(page, 0);
		int tamanhoNormalizado = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
		return PageRequest.of(paginaNormalizada, tamanhoNormalizado, DEFAULT_LIST_SORT);
	}

	private ExpenseResponse toResponse(Expense expense, List<Payment> payments) {
		ExpenseSnapshot snapshot = snapshot(expense, payments);
		return new ExpenseResponse(
			expense.getId(),
			expense.getDescription(),
			expense.getAmount(),
			expense.getDueDate(),
			expense.getContext(),
			new ReferenceResponse(expense.getCategoryId(), expense.getCategoryNameSnapshot()),
			new ReferenceResponse(expense.getSubcategoryId(), expense.getSubcategoryNameSnapshot()),
			expense.getNotes(),
			snapshot.status(),
			snapshot.paidAmount(),
			snapshot.remainingAmount(),
			snapshot.payments().size(),
			snapshot.status() == ExpenseStatus.VENCIDA,
			expense.getCreatedAt(),
			expense.getUpdatedAt()
		);
	}

	private ExpenseDetailResponse toDetailResponse(Expense expense, List<Payment> payments) {
		ExpenseSnapshot snapshot = snapshot(expense, payments);
		return new ExpenseDetailResponse(
			expense.getId(),
			expense.getDescription(),
			expense.getAmount(),
			expense.getDueDate(),
			expense.getContext(),
			new ReferenceResponse(expense.getCategoryId(), expense.getCategoryNameSnapshot()),
			new ReferenceResponse(expense.getSubcategoryId(), expense.getSubcategoryNameSnapshot()),
			expense.getNotes(),
			snapshot.status(),
			snapshot.paidAmount(),
			snapshot.remainingAmount(),
			snapshot.payments().size(),
			snapshot.status() == ExpenseStatus.VENCIDA,
			expense.getCreatedAt(),
			expense.getUpdatedAt(),
			snapshot.payments()
		);
	}

	private ExpenseSnapshot snapshot(Expense expense, List<Payment> payments) {
		List<Payment> orderedPayments = payments == null ? List.of() : payments;
		BigDecimal paidAmount = sumPaidAmount(orderedPayments);
		BigDecimal remainingAmount = expense.getAmount().subtract(paidAmount).max(BigDecimal.ZERO);
		ExpenseStatus status = statusCalculator.calcular(expense.getAmount(), paidAmount, expense.getDueDate(), LocalDate.now());
		List<PaymentResponse> responses = orderedPayments.stream()
			.map(payment -> new PaymentResponse(
				payment.getId(),
				payment.getExpenseId(),
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
		return new ExpenseSnapshot(status, paidAmount, remainingAmount, responses);
	}

	private void validateAmountAgainstPaidAmount(BigDecimal amount, List<Payment> payments) {
		BigDecimal paidAmount = sumPaidAmount(payments);
		if (amount.compareTo(paidAmount) < 0) {
			throw new IllegalArgumentException("Expense amount cannot be lower than the paid amount");
		}
	}

	private BigDecimal sumPaidAmount(List<Payment> payments) {
		return payments.stream()
			.map(Payment::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private Category requireCategory(Long householdId, Long categoryId) {
		Category category = categoryRepository.findById(householdId, categoryId)
			.orElseThrow(() -> new CategoryNotFoundException(categoryId));
		if (!category.isActive()) {
			throw new IllegalArgumentException("Category must be active");
		}
		return category;
	}

	private Subcategory requireSubcategory(Long householdId, Long subcategoryId, Long categoryId) {
		Subcategory subcategory = subcategoryRepository.findById(householdId, subcategoryId)
			.orElseThrow(() -> new SubcategoryNotFoundException(subcategoryId));
		if (!subcategory.isActive()) {
			throw new IllegalArgumentException("Subcategory must be active");
		}
		if (!subcategory.getCategoryId().equals(categoryId)) {
			throw new IllegalArgumentException("Subcategory must belong to the informed category");
		}
		return subcategory;
	}

	private String normalizeRequired(String value, String fieldName) {
		String normalized = normalizeOptional(value);
		if (!StringUtils.hasText(normalized)) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return normalized;
	}

	private String normalizeOptional(String value) {
		return value == null ? null : value.trim();
	}

	private BigDecimal requirePositive(BigDecimal value, String fieldName) {
		if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException(fieldName + " must be greater than zero");
		}
		return value;
	}

	private LocalDate requireDate(LocalDate dueDate) {
		if (dueDate == null) {
			throw new IllegalArgumentException("dueDate must not be null");
		}
		return dueDate;
	}

	private ExpenseContext requireContext(ExpenseContext context) {
		if (context == null) {
			throw new IllegalArgumentException("context must not be null");
		}
		return context;
	}

	private record ExpenseSnapshot(
		ExpenseStatus status,
		BigDecimal paidAmount,
		BigDecimal remainingAmount,
		List<PaymentResponse> payments
	) {
	}
}
