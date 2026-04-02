package com.gilrossi.despesas.fixedbill;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.api.v1.shared.FieldBusinessRuleException;
import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;
import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryNotFoundException;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.catalog.subcategory.Subcategory;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryNotFoundException;
import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.expense.Expense;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.expense.ExpenseResponse;
import com.gilrossi.despesas.expense.ExpenseStatus;
import com.gilrossi.despesas.expense.ExpenseStatusCalculator;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;
import com.gilrossi.despesas.spacereference.SpaceReference;
import com.gilrossi.despesas.spacereference.SpaceReferenceRepository;

@Service
public class FixedBillService {

	private final FixedBillRepository fixedBillRepository;
	private final CategoryRepository categoryRepository;
	private final SubcategoryRepository subcategoryRepository;
	private final SpaceReferenceRepository spaceReferenceRepository;
	private final ExpenseRepository expenseRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;
	private final ExpenseStatusCalculator expenseStatusCalculator;

	public FixedBillService(
		FixedBillRepository fixedBillRepository,
		CategoryRepository categoryRepository,
		SubcategoryRepository subcategoryRepository,
		SpaceReferenceRepository spaceReferenceRepository,
		ExpenseRepository expenseRepository,
		CurrentHouseholdProvider currentHouseholdProvider,
		ExpenseStatusCalculator expenseStatusCalculator
	) {
		this.fixedBillRepository = fixedBillRepository;
		this.categoryRepository = categoryRepository;
		this.subcategoryRepository = subcategoryRepository;
		this.spaceReferenceRepository = spaceReferenceRepository;
		this.expenseRepository = expenseRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
		this.expenseStatusCalculator = expenseStatusCalculator;
	}

	@Transactional
	public FixedBillResponse create(CreateFixedBillRequest request) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Category category = requireCategory(householdId, request.categoryId());
		Subcategory subcategory = requireSubcategory(householdId, request.subcategoryId(), category.getId());
		SpaceReference spaceReference = resolveSpaceReference(householdId, request.spaceReferenceId());
		FixedBill fixedBill = new FixedBill(
			householdId,
			normalizeRequired(request.description(), "description"),
			requirePositive(request.amount(), "amount"),
			requireDate(request.firstDueDate()),
			requireSupportedFrequency(request.frequency()),
			defaultContext(),
			category.getId(),
			category.getName(),
			subcategory.getId(),
			subcategory.getName(),
			spaceReference == null ? null : spaceReference.getId()
		);
		FixedBill saved = fixedBillRepository.save(fixedBill);
		return toResponse(saved, spaceReference, null);
	}

	@Transactional(readOnly = true)
	public List<FixedBillResponse> listActive() {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		List<FixedBill> fixedBills =
			fixedBillRepository.findAllByHouseholdIdAndActiveTrueOrderByCreatedAtDescIdDesc(householdId);
		Map<Long, SpaceReference> referencesById = loadReferencesById(householdId, fixedBills);
		Map<Long, Expense> latestExpensesByFixedBillId = loadLatestExpensesByFixedBillId(householdId, fixedBills);
		return fixedBills.stream()
			.map(fixedBill -> toResponse(
				fixedBill,
				referencesById.get(fixedBill.getSpaceReferenceId()),
				latestExpensesByFixedBillId.get(fixedBill.getId())
			))
			.sorted(Comparator
				.comparing(FixedBillResponse::nextDueDate)
				.thenComparing(FixedBillResponse::description, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(FixedBillResponse::id))
			.toList();
	}

	@Transactional(readOnly = true)
	public FixedBillResponse detail(Long id) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		FixedBill fixedBill = requireActiveFixedBill(householdId, id);
		SpaceReference spaceReference = fixedBill.getSpaceReferenceId() == null
			? null
			: spaceReferenceRepository.findById(householdId, fixedBill.getSpaceReferenceId()).orElse(null);
		Expense latestExpense = latestGeneratedExpense(householdId, fixedBill.getId());
		return toResponse(fixedBill, spaceReference, latestExpense);
	}

	@Transactional
	public FixedBillResponse update(Long id, UpdateFixedBillRequest request) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		FixedBill fixedBill = requireActiveFixedBillForUpdate(householdId, id);
		Expense latestExpense = latestGeneratedExpense(householdId, fixedBill.getId());
		validateEditableDates(fixedBill, latestExpense, request.firstDueDate());
		Category category = requireCategory(householdId, request.categoryId());
		Subcategory subcategory = requireSubcategory(householdId, request.subcategoryId(), category.getId());
		SpaceReference spaceReference = resolveSpaceReference(householdId, request.spaceReferenceId());

		fixedBill.setDescription(normalizeRequired(request.description(), "description"));
		fixedBill.setAmount(requirePositive(request.amount(), "amount"));
		fixedBill.setFirstDueDate(requireDate(request.firstDueDate()));
		fixedBill.setFrequency(requireSupportedFrequency(request.frequency()));
		fixedBill.setContext(defaultContext());
		fixedBill.setCategoryId(category.getId());
		fixedBill.setCategoryNameSnapshot(category.getName());
		fixedBill.setSubcategoryId(subcategory.getId());
		fixedBill.setSubcategoryNameSnapshot(subcategory.getName());
		fixedBill.setSpaceReferenceId(spaceReference == null ? null : spaceReference.getId());
		FixedBill saved = fixedBillRepository.save(fixedBill);
		return toResponse(saved, spaceReference, latestExpense);
	}

	@Transactional
	public void delete(Long id) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		FixedBill fixedBill = requireActiveFixedBillForUpdate(householdId, id);
		fixedBill.setActive(false);
		fixedBillRepository.save(fixedBill);
	}

	@Transactional
	public ExpenseResponse launchExpense(Long id) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		FixedBill fixedBill = requireActiveFixedBillForUpdate(householdId, id);
		Expense latestExpense = latestGeneratedExpense(householdId, fixedBill.getId());
		LocalDate nextDueDate = nextDueDate(fixedBill, latestExpense);
		Expense expense = new Expense(
			householdId,
			fixedBill.getDescription(),
			fixedBill.getAmount(),
			nextDueDate,
			nextDueDate,
			fixedBill.getContext(),
			fixedBill.getCategoryId(),
			fixedBill.getCategoryNameSnapshot(),
			fixedBill.getSubcategoryId(),
			fixedBill.getSubcategoryNameSnapshot(),
			null,
			fixedBill.getSpaceReferenceId()
		);
		expense.setFixedBillId(fixedBill.getId());
		Expense saved = expenseRepository.save(expense);
		SpaceReference spaceReference = fixedBill.getSpaceReferenceId() == null
			? null
			: spaceReferenceRepository.findById(householdId, fixedBill.getSpaceReferenceId()).orElse(null);
		ExpenseStatus status = expenseStatusCalculator.calcular(
			saved.getAmount(),
			BigDecimal.ZERO,
			saved.getDueDate(),
			LocalDate.now()
		);
		ReferenceResponse reference = toReference(spaceReference);
		return new ExpenseResponse(
			saved.getId(),
			saved.getDescription(),
			saved.getAmount(),
			saved.getDueDate(),
			saved.getOccurredOn(),
			new ReferenceResponse(saved.getCategoryId(), saved.getCategoryNameSnapshot()),
			new ReferenceResponse(saved.getSubcategoryId(), saved.getSubcategoryNameSnapshot()),
			reference,
			saved.getNotes(),
			status,
			BigDecimal.ZERO,
			saved.getAmount(),
			0,
			status == ExpenseStatus.VENCIDA,
			saved.getCreatedAt(),
			saved.getUpdatedAt()
		);
	}

	private Map<Long, SpaceReference> loadReferencesById(Long householdId, List<FixedBill> fixedBills) {
		return spaceReferenceRepository.findAllByIds(
			householdId,
			fixedBills.stream()
				.map(FixedBill::getSpaceReferenceId)
				.filter(id -> id != null)
				.toList()
		).stream().collect(Collectors.toMap(SpaceReference::getId, Function.identity()));
	}

	private Map<Long, Expense> loadLatestExpensesByFixedBillId(Long householdId, List<FixedBill> fixedBills) {
		List<Long> fixedBillIds = fixedBills.stream()
			.map(FixedBill::getId)
			.toList();
		if (fixedBillIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Expense> latest = new LinkedHashMap<>();
		for (Expense expense : expenseRepository.findActiveByHouseholdIdAndFixedBillIdInOrderByFixedBillIdAscEffectiveDateDescIdDesc(
			householdId,
			fixedBillIds
		)) {
			latest.putIfAbsent(expense.getFixedBillId(), expense);
		}
		return latest;
	}

	private Expense latestGeneratedExpense(Long householdId, Long fixedBillId) {
		return expenseRepository.findActiveByHouseholdIdAndFixedBillIdOrderByEffectiveDateDescIdDesc(
			householdId,
			fixedBillId
		).stream().findFirst().orElse(null);
	}

	private FixedBill requireActiveFixedBill(Long householdId, Long id) {
		return fixedBillRepository.findActiveByIdAndHouseholdId(id, householdId)
			.orElseThrow(() -> new FixedBillNotFoundException(id));
	}

	private FixedBill requireActiveFixedBillForUpdate(Long householdId, Long id) {
		return fixedBillRepository.findActiveByIdAndHouseholdIdForUpdate(id, householdId)
			.orElseThrow(() -> new FixedBillNotFoundException(id));
	}

	private void validateEditableDates(FixedBill fixedBill, Expense latestExpense, LocalDate requestedFirstDueDate) {
		if (latestExpense == null) {
			return;
		}
		if (!fixedBill.getFirstDueDate().equals(requestedFirstDueDate)) {
			throw new FieldBusinessRuleException(
				"firstDueDate",
				"firstDueDate cannot change after the fixed bill has already launched expenses"
			);
		}
	}

	private Category requireCategory(Long householdId, Long categoryId) {
		Category category = categoryRepository.findById(householdId, categoryId)
			.orElseThrow(() -> new CategoryNotFoundException(categoryId));
		if (!category.isActive()) {
			throw new FieldBusinessRuleException(
				"categoryId",
				"categoryId must refer to an active category"
			);
		}
		return category;
	}

	private Subcategory requireSubcategory(Long householdId, Long subcategoryId, Long categoryId) {
		Subcategory subcategory = subcategoryRepository.findById(householdId, subcategoryId)
			.orElseThrow(() -> new SubcategoryNotFoundException(subcategoryId));
		if (!subcategory.isActive()) {
			throw new FieldBusinessRuleException(
				"subcategoryId",
				"subcategoryId must refer to an active subcategory"
			);
		}
		if (!subcategory.getCategoryId().equals(categoryId)) {
			throw new FieldBusinessRuleException(
				"subcategoryId",
				"subcategoryId must belong to the informed category"
			);
		}
		return subcategory;
	}

	private SpaceReference resolveSpaceReference(Long householdId, Long spaceReferenceId) {
		if (spaceReferenceId == null) {
			return null;
		}
		return spaceReferenceRepository.findById(householdId, spaceReferenceId)
			.orElseThrow(() -> new FieldBusinessRuleException(
				"spaceReferenceId",
				"spaceReferenceId must belong to the active household"
			));
	}

	private FixedBillResponse toResponse(FixedBill fixedBill, SpaceReference spaceReference, Expense latestExpense) {
		ReferenceResponse category = new ReferenceResponse(
			fixedBill.getCategoryId(),
			fixedBill.getCategoryNameSnapshot()
		);
		ReferenceResponse subcategory = new ReferenceResponse(
			fixedBill.getSubcategoryId(),
			fixedBill.getSubcategoryNameSnapshot()
		);
		LocalDate nextDueDate = nextDueDate(fixedBill, latestExpense);
		return new FixedBillResponse(
			fixedBill.getId(),
			fixedBill.getDescription(),
			fixedBill.getAmount(),
			fixedBill.getFirstDueDate(),
			fixedBill.getFrequency(),
			category,
			subcategory,
			toReference(spaceReference),
			fixedBill.isActive(),
			fixedBill.getCreatedAt(),
			nextDueDate,
			operationalStatus(nextDueDate),
			latestExpense == null
				? null
				: new FixedBillGeneratedExpenseResponse(
					latestExpense.getId(),
					effectiveDate(latestExpense),
					latestExpense.getCreatedAt()
				)
		);
	}

	private ReferenceResponse toReference(SpaceReference spaceReference) {
		if (spaceReference == null) {
			return null;
		}
		return new ReferenceResponse(spaceReference.getId(), spaceReference.getName());
	}

	private LocalDate nextDueDate(FixedBill fixedBill, Expense latestExpense) {
		if (latestExpense == null) {
			return fixedBill.getFirstDueDate();
		}
		return advance(effectiveDate(latestExpense), fixedBill.getFrequency());
	}

	private LocalDate effectiveDate(Expense expense) {
		return expense.getDueDate() != null ? expense.getDueDate() : expense.getOccurredOn();
	}

	private LocalDate advance(LocalDate baseDate, FixedBillFrequency frequency) {
		return switch (frequency) {
			case WEEKLY -> baseDate.plusWeeks(1);
			case MONTHLY -> baseDate.plusMonths(1);
		};
	}

	private FixedBillOperationalStatus operationalStatus(LocalDate nextDueDate) {
		LocalDate today = LocalDate.now();
		if (nextDueDate.isBefore(today)) {
			return FixedBillOperationalStatus.OVERDUE;
		}
		if (nextDueDate.isEqual(today)) {
			return FixedBillOperationalStatus.DUE_TODAY;
		}
		return FixedBillOperationalStatus.UPCOMING;
	}

	private String normalizeRequired(String value, String fieldName) {
		String normalized = value == null ? null : value.trim();
		if (!StringUtils.hasText(normalized)) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return normalized;
	}

	private BigDecimal requirePositive(BigDecimal value, String fieldName) {
		if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException(fieldName + " must be greater than zero");
		}
		return value;
	}

	private LocalDate requireDate(LocalDate value) {
		if (value == null) {
			throw new IllegalArgumentException("firstDueDate must not be null");
		}
		return value;
	}

	private FixedBillFrequency requireSupportedFrequency(FixedBillFrequency value) {
		if (value == null) {
			throw new IllegalArgumentException("frequency must not be null");
		}
		if (value != FixedBillFrequency.MONTHLY && value != FixedBillFrequency.WEEKLY) {
			throw new FieldBusinessRuleException(
				"frequency",
				"frequency must be WEEKLY or MONTHLY in this release"
			);
		}
		return value;
	}

	private ExpenseContext defaultContext() {
		return ExpenseContext.GERAL;
	}
}
