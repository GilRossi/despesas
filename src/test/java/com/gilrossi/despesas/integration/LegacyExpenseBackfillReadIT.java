package com.gilrossi.despesas.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gilrossi.despesas.DespesasApplication;

@SpringBootTest(classes = DespesasApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("postgres-it")
class LegacyExpenseBackfillReadIT {

	private static final String ADMIN_DB_URL = System.getProperty("test.admin.db.url", "jdbc:postgresql://127.0.0.1:5432/postgres");
	private static final String DB_USERNAME = System.getProperty("test.db.username", System.getenv().getOrDefault("TEST_DB_USERNAME", "postgres"));
	private static final String DB_PASSWORD = System.getProperty("test.db.password", System.getenv().getOrDefault("TEST_DB_PASSWORD", "postgres"));
	private static final String DB_NAME = "despesas_legacy_read_it_" + UUID.randomUUID().toString().replace("-", "");
	private static final String DB_URL = "jdbc:postgresql://127.0.0.1:5432/" + DB_NAME;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		createDatabase();
		Flyway.configure()
			.dataSource(DB_URL, DB_USERNAME, DB_PASSWORD)
			.locations("classpath:db/migration")
			.target("1")
			.load()
			.migrate();

		try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
		     Statement statement = connection.createStatement()) {
			statement.execute("insert into tb_despesas (descricao, valor, data, categoria) values ('Mercado legado', 55.90, date '2026-03-10', 'Alimentacao')");
			statement.execute("insert into tb_despesas (descricao, valor, data, categoria) values ('Conta antiga', 120.00, date '2026-03-05', 'Moradia')");
		} catch (SQLException exception) {
			throw new IllegalStateException("Could not seed legacy expenses", exception);
		}

		Flyway.configure()
			.dataSource(DB_URL, DB_USERNAME, DB_PASSWORD)
			.locations("classpath:db/migration")
			.load()
			.migrate();

		registry.add("spring.datasource.url", () -> DB_URL);
		registry.add("spring.datasource.username", () -> DB_USERNAME);
		registry.add("spring.datasource.password", () -> DB_PASSWORD);
		registry.add("app.security.token-secret", () -> "legacy-read-it-secret");
	}

	@AfterAll
	static void cleanup() {
		try (Connection connection = DriverManager.getConnection(ADMIN_DB_URL, DB_USERNAME, DB_PASSWORD);
		     Statement statement = connection.createStatement()) {
			statement.execute("select pg_terminate_backend(pid) from pg_stat_activity where datname = '" + DB_NAME + "'");
			statement.execute("drop database if exists " + DB_NAME);
		} catch (SQLException exception) {
			throw new IllegalStateException("Could not drop temporary legacy read database", exception);
		}
	}

	@Test
	void deve_expor_despesas_legadas_migradas_na_api() throws Exception {
		String accessToken = loginApi("system@local.invalid", "password");

		mockMvc.perform(get("/api/v1/expenses")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[?(@.description == 'Mercado legado')]").exists())
			.andExpect(jsonPath("$.content[?(@.description == 'Conta antiga')]").exists());
	}

	@Test
	void deve_manter_tabela_legada_congelada_e_rastrear_backfill() {
		Integer legacyCount = jdbcTemplate.queryForObject("select count(*) from tb_despesas", Integer.class);
		Integer migratedCount = jdbcTemplate.queryForObject(
			"select count(*) from expenses where legacy_tb_despesa_id is not null",
			Integer.class
		);

		Assertions.assertEquals(2, legacyCount);
		Assertions.assertEquals(2, migratedCount);
	}

	@Test
	void deve_preservar_integridade_do_backfill_legado_sem_nulos_ou_duplicidade() {
		Integer migratedCount = jdbcTemplate.queryForObject(
			"select count(*) from expenses where legacy_tb_despesa_id is not null",
			Integer.class
		);
		Integer distinctLegacyIds = jdbcTemplate.queryForObject(
			"select count(distinct legacy_tb_despesa_id) from expenses where legacy_tb_despesa_id is not null",
			Integer.class
		);
		Integer missingCategoryRefs = jdbcTemplate.queryForObject(
			"select count(*) from expenses where legacy_tb_despesa_id is not null and category_id is null",
			Integer.class
		);
		Integer missingSubcategoryRefs = jdbcTemplate.queryForObject(
			"select count(*) from expenses where legacy_tb_despesa_id is not null and subcategory_id is null",
			Integer.class
		);
		Integer snapshotMismatch = jdbcTemplate.queryForObject(
			"""
				select count(*)
				from expenses e
				join tb_despesas t on t.id = e.legacy_tb_despesa_id
				where trim(t.categoria) <> e.category_name_snapshot
				   or e.subcategory_name_snapshot <> 'Sem Subcategoria'
			""",
			Integer.class
		);

		Assertions.assertEquals(migratedCount, distinctLegacyIds);
		Assertions.assertEquals(0, missingCategoryRefs);
		Assertions.assertEquals(0, missingSubcategoryRefs);
		Assertions.assertEquals(0, snapshotMismatch);
	}

	private String loginApi(String email, String password) throws Exception {
		String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
				.contentType("application/json")
				.content("""
					{
					  "email":"%s",
					  "password":"%s"
					}
					""".formatted(email, password)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		return objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();
	}

	private static void createDatabase() {
		try (Connection connection = DriverManager.getConnection(ADMIN_DB_URL, DB_USERNAME, DB_PASSWORD);
		     Statement statement = connection.createStatement()) {
			statement.execute("drop database if exists " + DB_NAME);
			statement.execute("create database " + DB_NAME);
		} catch (SQLException exception) {
			throw new IllegalStateException("Could not create temporary legacy read database", exception);
		}
	}
}
