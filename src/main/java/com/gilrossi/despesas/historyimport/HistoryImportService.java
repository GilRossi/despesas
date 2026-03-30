package com.gilrossi.despesas.historyimport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.api.v1.shared.FieldBusinessRuleException;
import com.gilrossi.despesas.api.v1.shared.FieldErrorResponse;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.expense.CreateExpenseRequest;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.expense.ExpenseService;
import com.gilrossi.despesas.payment.CreatePaymentRequest;
import com.gilrossi.despesas.payment.PaymentMethod;
import com.gilrossi.despesas.payment.PaymentResponse;
import com.gilrossi.despesas.payment.PaymentService;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class HistoryImportService {

	private static final String VALIDATION_ERROR_MESSAGE = "History import validation failed";

	private final ExpenseService expenseService;
	private final PaymentService paymentService;
	private final CategoryRepository categoryRepository;
	private final SubcategoryRepository subcategoryRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public HistoryImportService(
		ExpenseService expenseService,
		PaymentService paymentService,
		CategoryRepository categoryRepository,
		SubcategoryRepository subcategoryRepository,
		CurrentHouseholdProvider currentHouseholdProvider
	) {
		this.expenseService = expenseService;
		this.paymentService = paymentService;
		this.categoryRepository = categoryRepository;
		this.subcategoryRepository = subcategoryRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional
	public HistoryImportResponse importHistory(CreateHistoryImportRequest request) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		List<ValidatedHistoryImportEntry> validatedEntries = validateRequest(householdId, request);
		List<HistoryImportEntryResponse> importedEntries = validatedEntries.stream()
			.map(entry -> importEntry(householdId, request.paymentMethod(), entry))
			.toList();
		return new HistoryImportResponse(importedEntries.size(), importedEntries);
	}

	private List<ValidatedHistoryImportEntry> validateRequest(Long householdId, CreateHistoryImportRequest request) {
		List<FieldErrorResponse> fieldErrors = new ArrayList<>();
		if (request == null) {
			throw new IllegalArgumentException("request must not be null");
		}
		if (request.entries() == null || request.entries().isEmpty()) {
			fieldErrors.add(new FieldErrorResponse("entries", "entries must not be empty"));
		}
		if (request.paymentMethod() == null) {
			fieldErrors.add(new FieldErrorResponse("paymentMethod", "paymentMethod must not be null"));
		}
		if (!fieldErrors.isEmpty()) {
			throw new FieldBusinessRuleException(VALIDATION_ERROR_MESSAGE, fieldErrors);
		}

		List<ValidatedHistoryImportEntry> validatedEntries = new ArrayList<>();
		for (int index = 0; index < request.entries().size(); index++) {
			HistoryImportEntryRequest entry = request.entries().get(index);
			String fieldPrefix = "entries[" + index + "]";

			String description = normalizeRequired(entry.description());
			if (description == null) {
				fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".description", "description must not be blank"));
			}

			BigDecimal amount = requirePositive(entry.amount());
			if (amount == null) {
				fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".amount", "amount must be greater than zero"));
			}

			LocalDate date = requireDate(entry.date());
			if (date == null) {
				fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".date", "date must not be null"));
			}

			ExpenseContext context = requireContext(entry.context());
			if (context == null) {
				fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".context", "context must not be null"));
			}

			String notes = normalizeOptional(entry.notes());
			if (notes != null && notes.length() > 255) {
				fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".notes", "notes must have at most 255 characters"));
			}

			Category category = resolveCategory(householdId, entry.categoryId(), fieldPrefix, fieldErrors);
			Subcategory subcategory = resolveSubcategory(householdId, entry.subcategoryId(), category, fieldPrefix, fieldErrors);

			if (description != null
				&& amount != null
				&& date != null
				&& context != null
				&& category != null
				&& subcategory != null
				&& (notes == null || notes.length() <= 255)) {
				validatedEntries.add(new ValidatedHistoryImportEntry(
					description,
					amount,
					date,
					context,
					category.getId(),
					subcategory.getId(),
					notes
				));
			}
		}

		if (!fieldErrors.isEmpty()) {
			throw new FieldBusinessRuleException(VALIDATION_ERROR_MESSAGE, fieldErrors);
		}
		return validatedEntries;
	}

	private Category resolveCategory(
		Long householdId,
		Long categoryId,
		String fieldPrefix,
		List<FieldErrorResponse> fieldErrors
	) {
		if (categoryId == null) {
			fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".categoryId", "categoryId must not be null"));
			return null;
		}
		Category category = categoryRepository.findById(householdId, categoryId).orElse(null);
		if (category == null) {
			fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".categoryId", "categoryId must belong to the active household"));
			return null;
		}
		if (!category.isActive()) {
			fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".categoryId", "categoryId must refer to an active category"));
			return null;
		}
		return category;
	}

	private Subcategory resolveSubcategory(
		Long householdId,
		Long subcategoryId,
		Category category,
		String fieldPrefix,
		List<FieldErrorResponse> fieldErrors
	) {
		if (subcategoryId == null) {
			fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".subcategoryId", "subcategoryId must not be null"));
			return null;
		}
		Subcategory subcategory = subcategoryRepository.findById(householdId, subcategoryId).orElse(null);
		if (subcategory == null) {
			fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".subcategoryId", "subcategoryId must belong to the active household"));
			return null;
		}
		if (!subcategory.isActive()) {
			fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".subcategoryId", "subcategoryId must refer to an active subcategory"));
			return null;
		}
		if (category != null && !subcategory.getCategoryId().equals(category.getId())) {
			fieldErrors.add(new FieldErrorResponse(fieldPrefix + ".subcategoryId", "subcategoryId must belong to the informed category"));
			return null;
		}
		return subcategory;
	}

	private HistoryImportEntryResponse importEntry(
		Long householdId,
		PaymentMethod paymentMethod,
		ValidatedHistoryImportEntry entry
	) {
		ExpenseResponse createdExpense = expenseService.criarParaHousehold(householdId, new CreateExpenseRequest(
			entry.description(),
			entry.amount(),
			entry.date(),
			entry.date(),
			entry.context(),
			entry.categoryId(),
			entry.subcategoryId(),
			null,
			entry.notes()
		));
		PaymentResponse createdPayment = paymentService.registrar(new CreatePaymentRequest(
			createdExpense.id(),
			entry.amount(),
			entry.date(),
			paymentMethod,
			null
		));
		return new HistoryImportEntryResponse(
			createdExpense.id(),
			createdPayment.id(),
			createdExpense.description(),
			createdExpense.amount(),
			createdExpense.dueDate(),
			createdPayment.expenseStatus()
		);
	}

	private String normalizeRequired(String value) {
		String normalized = value == null ? null : value.trim();
		return StringUtils.hasText(normalized) ? normalized : null;
	}

	private String normalizeOptional(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private BigDecimal requirePositive(BigDecimal value) {
		return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
	}

	private LocalDate requireDate(LocalDate value) {
		return value;
	}

	private ExpenseContext requireContext(ExpenseContext value) {
		return value;
	}

	private record ValidatedHistoryImportEntry(
		String description,
		BigDecimal amount,
		LocalDate date,
		ExpenseContext context,
		Long categoryId,
		Long subcategoryId,
		String notes
	) {
	}
}
