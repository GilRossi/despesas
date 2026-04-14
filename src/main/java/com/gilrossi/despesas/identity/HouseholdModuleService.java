package com.gilrossi.despesas.identity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HouseholdModuleService {

	private final HouseholdRepository householdRepository;
	private final HouseholdModuleRepository householdModuleRepository;

	public HouseholdModuleService(
		HouseholdRepository householdRepository,
		HouseholdModuleRepository householdModuleRepository
	) {
		this.householdRepository = householdRepository;
		this.householdModuleRepository = householdModuleRepository;
	}

	@Transactional
	public List<HouseholdModule> initializeForHousehold(Long householdId, Set<HouseholdModuleKey> enabledModules) {
		return saveModules(householdId, enabledModules);
	}

	@Transactional
	public List<HouseholdModule> updateModules(Long householdId, Set<HouseholdModuleKey> enabledModules) {
		return saveModules(householdId, enabledModules);
	}

	@Transactional(readOnly = true)
	public List<HouseholdModule> listModules(Long householdId) {
		Household household = householdRepository.findByIdAndDeletedAtIsNull(householdId)
			.orElseThrow(() -> new IllegalArgumentException("Space was not found"));
		Map<HouseholdModuleKey, HouseholdModule> existingByKey = householdModuleRepository
			.findAllByHouseholdIdOrderByModuleKeyAsc(householdId)
			.stream()
			.collect(Collectors.toMap(HouseholdModule::getModuleKey, Function.identity()));

		List<HouseholdModule> modules = new ArrayList<>();
		for (HouseholdModuleKey moduleKey : HouseholdModuleKey.values()) {
			HouseholdModule existing = existingByKey.get(moduleKey);
			if (existing != null) {
				modules.add(existing);
				continue;
			}
			modules.add(new HouseholdModule(
				household,
				moduleKey,
				normalizeEnabledModules(Set.of()).contains(moduleKey)
			));
		}
		return modules;
	}

	@Transactional(readOnly = true)
	public List<HouseholdModule> listForHouseholds(List<Long> householdIds) {
		if (householdIds == null || householdIds.isEmpty()) {
			return List.of();
		}
		return householdModuleRepository.findAllByHouseholdIdInOrderByHouseholdIdAscModuleKeyAsc(householdIds);
	}

	@Transactional(readOnly = true)
	public boolean isEnabled(Long householdId, HouseholdModuleKey moduleKey) {
		return householdModuleRepository.findByHouseholdIdAndModuleKey(householdId, moduleKey)
			.map(HouseholdModule::isEnabled)
			.orElseGet(() -> normalizeEnabledModules(Set.of()).contains(moduleKey));
	}

	private List<HouseholdModule> saveModules(Long householdId, Set<HouseholdModuleKey> enabledModules) {
		Household household = householdRepository.findByIdAndDeletedAtIsNull(householdId)
			.orElseThrow(() -> new IllegalArgumentException("Space was not found"));
		Set<HouseholdModuleKey> normalizedEnabledModules = normalizeEnabledModules(enabledModules);
		Map<HouseholdModuleKey, HouseholdModule> existingByKey = householdModuleRepository
			.findAllByHouseholdIdOrderByModuleKeyAsc(householdId)
			.stream()
			.collect(Collectors.toMap(HouseholdModule::getModuleKey, Function.identity()));

		List<HouseholdModule> modulesToSave = new ArrayList<>();
		for (HouseholdModuleKey moduleKey : HouseholdModuleKey.values()) {
			HouseholdModule module = existingByKey.get(moduleKey);
			if (module == null) {
				module = new HouseholdModule(household, moduleKey, normalizedEnabledModules.contains(moduleKey));
			} else {
				module.setEnabled(normalizedEnabledModules.contains(moduleKey));
			}
			modulesToSave.add(module);
		}
		return householdModuleRepository.saveAll(modulesToSave).stream()
			.sorted((left, right) -> left.getModuleKey().compareTo(right.getModuleKey()))
			.toList();
	}

	private Set<HouseholdModuleKey> normalizeEnabledModules(Set<HouseholdModuleKey> enabledModules) {
		Set<HouseholdModuleKey> normalized = enabledModules == null || enabledModules.isEmpty()
			? EnumSet.noneOf(HouseholdModuleKey.class)
			: EnumSet.copyOf(enabledModules);
		for (HouseholdModuleKey moduleKey : HouseholdModuleKey.values()) {
			if (moduleKey.isMandatory()) {
				normalized.add(moduleKey);
			}
		}
		return normalized;
	}
}
