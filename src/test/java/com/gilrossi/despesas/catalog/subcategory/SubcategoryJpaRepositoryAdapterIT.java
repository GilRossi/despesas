package com.gilrossi.despesas.catalog.subcategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.gilrossi.despesas.catalog.category.Category;
import com.gilrossi.despesas.catalog.category.JpaCategoryRepositoryAdapter;

@SpringBootTest
@ActiveProfiles("postgres-it")
class SubcategoryJpaRepositoryAdapterIT {

	private static final Long HOUSEHOLD_ID = 1L;

	@Autowired
	private JpaSubcategoryRepositoryAdapter repository;

	@Autowired
	private JpaCategoryRepositoryAdapter categoryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("delete from payments");
		jdbcTemplate.execute("delete from expenses");
		jdbcTemplate.execute("delete from subcategories");
		jdbcTemplate.execute("delete from categories");
	}

	@Test
	void deve_salvar_filtrar_e_buscar_subcategoria_no_postgresql_real() {
		Category casa = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Casa", true));
		Category carro = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Carro", true));

		repository.save(HOUSEHOLD_ID, new Subcategory(null, casa.getId(), "Mercado", true));
		repository.save(HOUSEHOLD_ID, new Subcategory(null, casa.getId(), "Padaria", true));
		repository.save(HOUSEHOLD_ID, new Subcategory(null, carro.getId(), "Gasolina", true));

		var pagina = repository.findAll(HOUSEHOLD_ID, casa.getId(), "mer", true, PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))));

		assertEquals(1, pagina.getTotalElements());
		assertEquals(List.of("Mercado"), pagina.getContent().stream().map(Subcategory::getName).toList());
		assertEquals(casa.getId(), repository.findById(HOUSEHOLD_ID, pagina.getContent().get(0).getId()).orElseThrow().getCategoryId());
	}

	@Test
	void deve_rejeitar_subcategoria_ativa_duplicada_na_mesma_categoria() {
		Category casa = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Casa", true));
		repository.save(HOUSEHOLD_ID, new Subcategory(null, casa.getId(), "Mercado", true));

		assertThrows(DataIntegrityViolationException.class, () -> repository.save(HOUSEHOLD_ID, new Subcategory(null, casa.getId(), "mercado", true)));
	}

	@Test
	void deve_desativar_subcategorias_por_categoria() {
		Category casa = categoryRepository.save(HOUSEHOLD_ID, new Category(null, "Casa", true));
		Subcategory subcategoria = repository.save(HOUSEHOLD_ID, new Subcategory(null, casa.getId(), "Mercado", true));

		repository.desativarPorCategoriaId(HOUSEHOLD_ID, casa.getId());

		assertFalse(repository.findById(HOUSEHOLD_ID, subcategoria.getId()).orElseThrow().isActive());
	}
}
