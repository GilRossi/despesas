package com.gilrossi.despesas.catalog.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.gilrossi.despesas.catalog.subcategory.SubcategoryRepository;
import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@Service
public class CategoryService {

	private static final int TAMANHO_MAXIMO_PAGINA = 20;

	private final CategoryRepository categoryRepository;
	private final SubcategoryRepository subcategoryRepository;
	private final CurrentHouseholdProvider currentHouseholdProvider;

	public CategoryService(CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository, CurrentHouseholdProvider currentHouseholdProvider) {
		this.categoryRepository = categoryRepository;
		this.subcategoryRepository = subcategoryRepository;
		this.currentHouseholdProvider = currentHouseholdProvider;
	}

	@Transactional(readOnly = true)
	public Page<Category> listar(String q, Boolean active, int page, int size) {
		return categoryRepository.findAll(currentHouseholdProvider.requireHouseholdId(), q, active, paginaRequest(page, size));
	}

	@Transactional(readOnly = true)
	public Category buscarPorId(Long id) {
		return categoryRepository.findById(currentHouseholdProvider.requireHouseholdId(), id)
			.orElseThrow(() -> new CategoryNotFoundException(id));
	}

	@Transactional
	@PreAuthorize("hasRole('OWNER')")
	public Category criar(CreateCategoryCommand command) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		String name = sanitizeName(command.name());
		boolean active = command.active() == null || command.active();
		ensureNameIsValid(name);
		ensureUniqueName(householdId, name, null);

		Category categoria = new Category(null, name, active);
		return categoryRepository.save(householdId, categoria);
	}

	@Transactional
	@PreAuthorize("hasRole('OWNER')")
	public Category atualizar(Long id, UpdateCategoryCommand command) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Category categoria = categoryRepository.findById(householdId, id)
			.orElseThrow(() -> new CategoryNotFoundException(id));
		String name = sanitizeName(command.name());
		boolean active = command.active() == null || command.active();
		ensureNameIsValid(name);
		ensureUniqueName(householdId, name, id);

		categoria.setName(name);
		categoria.setActive(active);
		Category atualizada = categoryRepository.save(householdId, categoria);
		if (!atualizada.isActive()) {
			subcategoryRepository.desativarPorCategoriaId(householdId, atualizada.getId());
		}
		return atualizada;
	}

	@Transactional
	@PreAuthorize("hasRole('OWNER')")
	public void desativar(Long id) {
		Long householdId = currentHouseholdProvider.requireHouseholdId();
		Category categoria = categoryRepository.findById(householdId, id)
			.orElseThrow(() -> new CategoryNotFoundException(id));
		categoria.setActive(false);
		categoryRepository.save(householdId, categoria);
		subcategoryRepository.desativarPorCategoriaId(householdId, id);
	}

	private void ensureNameIsValid(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Category name must not be blank");
		}
	}

	private void ensureUniqueName(Long householdId, String name, Long id) {
		if (categoryRepository.existsByNameIgnoreCaseAndIdNotAndActiveTrue(householdId, name, id)) {
			throw new DuplicateCategoryException(name);
		}
	}

	private String sanitizeName(String name) {
		return name == null ? null : name.trim();
	}

	private PageRequest paginaRequest(int page, int size) {
		int paginaNormalizada = Math.max(page, 0);
		int tamanhoNormalizado = Math.max(1, Math.min(size, TAMANHO_MAXIMO_PAGINA));
		return PageRequest.of(paginaNormalizada, tamanhoNormalizado, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")));
	}
}
