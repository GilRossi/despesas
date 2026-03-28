package com.gilrossi.despesas.income;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.api.v1.shared.FieldBusinessRuleException;
import com.gilrossi.despesas.api.v1.shared.ReferenceResponse;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;
import com.gilrossi.despesas.spacereference.SpaceReference;
import com.gilrossi.despesas.spacereference.SpaceReferenceRepository;

@Service
public class IncomeService {

	private final IncomeRepository incomeRepository;
	private final SpaceReferenceRepository spaceReferenceRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public IncomeService(
		IncomeRepository incomeRepository,
		SpaceReferenceRepository spaceReferenceRepository,
		CurrentHouseholdProvider currentHouseholdProvider
	) {
		this.incomeRepository = incomeRepository;
		this.spaceReferenceRepository = spaceReferenceRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional
	public IncomeResponse create(CreateIncomeRequest request) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		SpaceReference spaceReference = resolveSpaceReference(householdId, request.spaceReferenceId());
		Income income = new Income(
			householdId,
			normalizeRequired(request.description(), "description"),
			requirePositive(request.amount(), "amount"),
			requireDate(request.receivedOn()),
			spaceReference == null ? null : spaceReference.getId()
		);
		Income saved = incomeRepository.save(income);
		return toResponse(saved, spaceReference);
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

	private IncomeResponse toResponse(Income income, SpaceReference spaceReference) {
		ReferenceResponse reference = spaceReference == null
			? null
			: new ReferenceResponse(spaceReference.getId(), spaceReference.getName());
		return new IncomeResponse(
			income.getId(),
			income.getDescription(),
			income.getAmount(),
			income.getReceivedOn(),
			reference,
			income.getCreatedAt()
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
			throw new IllegalArgumentException("receivedOn must not be null");
		}
		return value;
	}
}
