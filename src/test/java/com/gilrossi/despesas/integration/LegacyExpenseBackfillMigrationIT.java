package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class LegacyExpenseBackfillMigrationIT {

	private static final String ADMIN_DB_URL = System.getProperty("test.admin.db.url", "jdbc:postgresql://127.0.0.1:5432/postgres");
	private static final String DB_USERNAME = System.getProperty("test.db.username", System.getenv().getOrDefault("TEST_DB_USERNAME", "postgres"));
	private static final String DB_PASSWORD = System.getProperty("test.db.password", System.getenv().getOrDefault("TEST_DB_PASSWORD", "postgres"));
	private static final String DB_NAME = "despesas_legacy_backfill_it_" + UUID.randomUUID().toString().replace("-", "");
	private static final String DB_URL = "jdbc:postgresql://127.0.0.1:5432/" + DB_NAME;

	@AfterAll
	static void cleanup() {
		try (Connection connection = DriverManager.getConnection(ADMIN_DB_URL, DB_USERNAME, DB_PASSWORD);
		     Statement statement = connection.createStatement()) {
			statement.execute("select pg_terminate_backend(pid) from pg_stat_activity where datname = '" + DB_NAME + "'");
			statement.execute("drop database if exists " + DB_NAME);
		} catch (SQLException exception) {
			throw new IllegalStateException("Could not drop temporary legacy backfill test database", exception);
		}
	}

	@Test
	void deve_migrar_tb_despesas_para_expenses_sem_perder_historico() throws Exception {
		createDatabase();
		Flyway.configure()
			.dataSource(DB_URL, DB_USERNAME, DB_PASSWORD)
			.locations("classpath:db/migration")
			.target("1")
			.load()
			.migrate();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(DB_URL, DB_USERNAME, DB_PASSWORD));
		jdbcTemplate.update(
			"insert into tb_despesas (descricao, valor, data, categoria) values (?, ?, ?, ?)",
			"Mercado antigo",
			50.25,
			java.sql.Date.valueOf("2026-03-01"),
			"Alimentação"
		);
		jdbcTemplate.update(
			"insert into tb_despesas (descricao, valor, data, categoria) values (?, ?, ?, ?)",
			"Internet antiga",
			120.00,
			java.sql.Date.valueOf("2026-03-02"),
			"Casa"
		);

		Flyway.configure()
			.dataSource(DB_URL, DB_USERNAME, DB_PASSWORD)
			.locations("classpath:db/migration")
			.load()
			.migrate();

		assertThat(jdbcTemplate.queryForObject("select count(*) from expenses", Integer.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("select count(*) from legacy_expense_migration_map", Integer.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("select min(household_id) from expenses", Long.class)).isEqualTo(1L);
		assertThat(jdbcTemplate.queryForObject(
			"select category_name_snapshot from expenses where description = 'Mercado antigo'",
			String.class
		)).isEqualTo("Alimentação");
		assertThat(jdbcTemplate.queryForObject(
			"select subcategory_name_snapshot from expenses where description = 'Mercado antigo'",
			String.class
		)).isEqualTo("Sem subcategoria (legado)");
	}

	private static void createDatabase() {
		try (Connection connection = DriverManager.getConnection(ADMIN_DB_URL, DB_USERNAME, DB_PASSWORD);
		     Statement statement = connection.createStatement()) {
			statement.execute("drop database if exists " + DB_NAME);
			statement.execute("create database " + DB_NAME);
		} catch (SQLException exception) {
			throw new IllegalStateException("Could not create temporary legacy backfill test database", exception);
		}
	}
}
