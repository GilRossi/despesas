package com.gilrossi.despesas.catalog.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

@SpringBootTest
@ActiveProfiles("postgres-it")
class CategoryJpaRepositoryAdapterIT {

	private static final Long HOUSEHOLD_ID = 1L;

	@Autowired
	private JpaCategoryRepositoryAdapter repository;

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
	void deve_salvar_listar_e_buscar_categoria_no_postgresql_real() {
		Category categoriaCasa = repository.save(HOUSEHOLD_ID, new Category(null, " Casa ", true));
		repository.save(HOUSEHOLD_ID, new Category(null, "Carro", false));
		repository.save(HOUSEHOLD_ID, new Category(null, "Pets", true));

		var pagina = repository.findAll(HOUSEHOLD_ID, "ca", true, PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id"))));

		assertEquals(1, pagina.getTotalElements());
		assertEquals(List.of("Casa"), pagina.getContent().stream().map(Category::getName).toList());
		assertTrue(repository.findById(HOUSEHOLD_ID, categoriaCasa.getId()).orElseThrow().isActive());
		assertEquals("Casa", repository.findByNameIgnoreCase(HOUSEHOLD_ID, "cAsA").orElseThrow().getName());
	}

	@Test
	void deve_rejeitar_categoria_ativa_duplicada_na_mesma_unidade() {
		repository.save(HOUSEHOLD_ID, new Category(null, "Casa", true));

		assertThrows(DataIntegrityViolationException.class, () -> repository.save(HOUSEHOLD_ID, new Category(null, "casa", true)));
	}

	@Test
	void deve_manter_categoria_inativa_duplicada_quando_nao_conflitar_com_ativa() {
		repository.save(HOUSEHOLD_ID, new Category(null, "Casa", true));
		Category inativa = repository.save(HOUSEHOLD_ID, new Category(null, "casa", false));

		assertFalse(inativa.isActive());
		assertEquals(2, repository.findAll(HOUSEHOLD_ID, null, null, PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")))).getTotalElements());
	}
}
