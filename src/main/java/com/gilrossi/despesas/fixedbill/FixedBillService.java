package com.gilrossi.despesas.fixedbill;

import java.math.BigDecimal;
import java.time.LocalDate;

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
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;
import com.gilrossi.despesas.spacereference.SpaceReference;
import com.gilrossi.despesas.spacereference.SpaceReferenceRepository;

@Service
public class FixedBillService {

	private final FixedBillRepository fixedBillRepository;
	private final CategoryRepository categoryRepository;
	private final SubcategoryRepository subcategoryRepository;
	private final SpaceReferenceRepository spaceReferenceRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public FixedBillService(
		FixedBillRepository fixedBillRepository,
		CategoryRepository categoryRepository,
		SubcategoryRepository subcategoryRepository,
		SpaceReferenceRepository spaceReferenceRepository,
		CurrentHouseholdProvider currentHouseholdProvider
	) {
		this.fixedBillRepository = fixedBillRepository;
		this.categoryRepository = categoryRepository;
		this.subcategoryRepository = subcategoryRepository;
		this.spaceReferenceRepository = spaceReferenceRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
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
		return toResponse(saved, spaceReference);
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

	private FixedBillResponse toResponse(FixedBill fixedBill, SpaceReference spaceReference) {
		ReferenceResponse category = new ReferenceResponse(
			fixedBill.getCategoryId(),
			fixedBill.getCategoryNameSnapshot()
		);
		ReferenceResponse subcategory = new ReferenceResponse(
			fixedBill.getSubcategoryId(),
			fixedBill.getSubcategoryNameSnapshot()
		);
		ReferenceResponse reference = spaceReference == null
			? null
			: new ReferenceResponse(spaceReference.getId(), spaceReference.getName());
		return new FixedBillResponse(
			fixedBill.getId(),
			fixedBill.getDescription(),
			fixedBill.getAmount(),
			fixedBill.getFirstDueDate(),
			fixedBill.getFrequency(),
			category,
			subcategory,
			reference,
			fixedBill.isActive(),
			fixedBill.getCreatedAt()
		);
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
