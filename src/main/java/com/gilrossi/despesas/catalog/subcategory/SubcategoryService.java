package com.gilrossi.despesas.catalog.subcategory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.CategoryNotFoundException;
import com.gilrossi.despesas.catalog.category.CategoryRepository;
import com.gilrossi.despesas.expense.ExpenseRepository;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class SubcategoryService {

	private static final int TAMANHO_MAXIMO_PAGINA = 20;

	private final SubcategoryRepository subcategoryRepository;
	private final CategoryRepository categoryRepository;
	private final ExpenseRepository expenseRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public SubcategoryService(SubcategoryRepository subcategoryRepository, CategoryRepository categoryRepository, ExpenseRepository expenseRepository, CurrentHouseholdProvider currentHouseholdProvider) {
		this.subcategoryRepository = subcategoryRepository;
		this.categoryRepository = categoryRepository;
		this.expenseRepository = expenseRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional(readOnly = true)
	public Page<Subcategory> listar(Long categoryId, String q, Boolean active, int page, int size) {
		return subcategoryRepository.findAll(currentHouseholdProvider.requireHouseholdId(), categoryId, q, active, paginaRequest(page, size));
	}

	@Transactional(readOnly = true)
	public Subcategory buscarPorId(Long id) {
		return subcategoryRepository.findById(currentHouseholdProvider.requireHouseholdId(), id)
			.orElseThrow(() -> new SubcategoryNotFoundException(id));
	}

	@Transactional
	@PreAuthorize("hasRole('OWNER')")
	public Subcategory criar(CreateSubcategoryCommand command) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Category category = categoriaAtiva(householdId, command.categoryId());
		String name = sanitizeName(command.name());
		boolean active = command.active() == null || command.active();
		ensureNameIsValid(name);
		ensureUniqueName(householdId, command.categoryId(), name, null);

		Subcategory subcategory = new Subcategory(null, category.getId(), name, active);
		return subcategoryRepository.save(householdId, subcategory);
	}

	@Transactional
	@PreAuthorize("hasRole('OWNER')")
	public Subcategory atualizar(Long id, UpdateSubcategoryCommand command) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Subcategory subcategory = subcategoryRepository.findById(householdId, id)
			.orElseThrow(() -> new SubcategoryNotFoundException(id));
		Category category = categoriaAtiva(householdId, command.categoryId());
		String name = sanitizeName(command.name());
		boolean active = command.active() == null || command.active();
		ensureNameIsValid(name);
		ensureUniqueName(householdId, category.getId(), name, id);
		validateCategoryChange(householdId, subcategory, category.getId());

		subcategory.setCategoryId(category.getId());
		subcategory.setName(name);
		subcategory.setActive(active);
		return subcategoryRepository.save(householdId, subcategory);
	}

	@Transactional
	@PreAuthorize("hasRole('OWNER')")
	public void desativar(Long id) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Subcategory subcategory = subcategoryRepository.findById(householdId, id)
			.orElseThrow(() -> new SubcategoryNotFoundException(id));
		subcategory.setActive(false);
		subcategoryRepository.save(householdId, subcategory);
	}

	private Category categoriaAtiva(Long householdId, Long categoryId) {
		Category category = categoryRepository.findById(householdId, categoryId)
			.orElseThrow(() -> new CategoryNotFoundException(categoryId));
		if (!category.isActive()) {
			throw new IllegalArgumentException("Category must be active");
		}
		return category;
	}

	private void ensureNameIsValid(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Subcategory name must not be blank");
		}
	}

	private void ensureUniqueName(Long householdId, Long categoryId, String name, Long id) {
		if (subcategoryRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNotAndActiveTrue(householdId, categoryId, name, id)) {
			throw new DuplicateSubcategoryException(name);
		}
	}

	private String sanitizeName(String name) {
		return name == null ? null : name.trim();
	}

	private void validateCategoryChange(Long householdId, Subcategory subcategory, Long newCategoryId) {
		if (!subcategory.getCategoryId().equals(newCategoryId) && expenseRepository.existsByHouseholdIdAndSubcategoryId(householdId, subcategory.getId())) {
			throw new IllegalStateException("Subcategory with linked expenses cannot change category");
		}
	}

	private PageRequest paginaRequest(int page, int size) {
		int paginaNormalizada = Math.max(page, 0);
		int tamanhoNormalizado = Math.max(1, Math.min(size, TAMANHO_MAXIMO_PAGINA));
		return PageRequest.of(paginaNormalizada, tamanhoNormalizado, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
	}
}
