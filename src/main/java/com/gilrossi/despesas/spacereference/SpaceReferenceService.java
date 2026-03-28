package com.gilrossi.despesas.spacereference;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class SpaceReferenceService {

	static final String DUPLICATE_SUGGESTION_MESSAGE =
		"Encontrei uma referência parecida no seu Espaço. Quer usar essa para evitar duplicidade?";

	private final SpaceReferenceRepository repository;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public SpaceReferenceService(
		SpaceReferenceRepository repository,
		CurrentHouseholdProvider currentHouseholdProvider
	) {
		this.repository = repository;
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional(readOnly = true)
	public List<SpaceReference> list(SpaceReferenceTypeGroup typeGroup, SpaceReferenceType type, String q) {
		if (type != null && typeGroup != null && type.group() != typeGroup) {
			return List.of();
		}
		return repository.findAll(currentHouseholdProvider.requireHouseholdId(), typeGroup, type, q);
	}

	@Transactional
	@PreAuthorize("hasRole('OWNER')")
	public SpaceReferenceCreateResult create(CreateSpaceReferenceCommand command) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		if (command.type() == null) {
			throw new IllegalArgumentException("type must not be null");
		}

		String displayName = SpaceReferenceNormalizer.sanitizeDisplayName(command.name());
		if (!StringUtils.hasText(displayName)) {
			throw new IllegalArgumentException("name must not be blank");
		}

		String normalizedName = SpaceReferenceNormalizer.normalizeName(displayName);
		SpaceReferenceType type = command.type();

		return repository.findByTypeAndNormalizedName(householdId, type, normalizedName)
			.map(existing -> SpaceReferenceCreateResult.duplicateSuggestion(existing, DUPLICATE_SUGGESTION_MESSAGE))
			.orElseGet(() -> createSafely(householdId, type, displayName, normalizedName));
	}

	private SpaceReferenceCreateResult createSafely(
		Long householdId,
		SpaceReferenceType type,
		String displayName,
		String normalizedName
	) {
		SpaceReference reference = new SpaceReference(
			null,
			householdId,
			type,
			displayName,
			normalizedName,
			null,
			null,
			null
		);
		try {
			return SpaceReferenceCreateResult.created(repository.save(reference));
		} catch (DataIntegrityViolationException exception) {
			return repository.findByTypeAndNormalizedName(householdId, type, normalizedName)
				.map(existing -> SpaceReferenceCreateResult.duplicateSuggestion(existing, DUPLICATE_SUGGESTION_MESSAGE))
				.orElseThrow(() -> exception);
		}
	}
}
