package com.gilrossi.despesas.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.gilrossi.despesas.DespesasApplication;

@SpringBootTest(classes = DespesasApplication.class)
@ActiveProfiles("postgres-it")
class FlywayMigrationIT {

	private static final String ADMIN_DB_URL = System.getProperty("test.admin.db.url", "jdbc:postgresql://127.0.0.1:5432/postgres");
	private static final String DB_USERNAME = System.getProperty("test.db.username", System.getenv().getOrDefault("TEST_DB_USERNAME", "postgres"));
	private static final String DB_PASSWORD = System.getProperty("test.db.password", System.getenv().getOrDefault("TEST_DB_PASSWORD", "postgres"));
	private static final String DB_NAME = "despesas_flyway_it_" + UUID.randomUUID().toString().replace("-", "");
	private static final String DB_URL = "jdbc:postgresql://127.0.0.1:5432/" + DB_NAME;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		createDatabase();
		registry.add("spring.datasource.url", () -> DB_URL);
		registry.add("spring.datasource.username", () -> DB_USERNAME);
		registry.add("spring.datasource.password", () -> DB_PASSWORD);
	}

	@AfterAll
	static void cleanup() {
		try (Connection connection = DriverManager.getConnection(ADMIN_DB_URL, DB_USERNAME, DB_PASSWORD);
		     Statement statement = connection.createStatement()) {
			statement.execute("select pg_terminate_backend(pid) from pg_stat_activity where datname = '" + DB_NAME + "'");
			statement.execute("drop database if exists " + DB_NAME);
		} catch (SQLException exception) {
			throw new IllegalStateException("Could not drop temporary Flyway test database", exception);
		}
	}

	@Test
	void deveCriarTabelasPrincipaisDaFaseUm() {
		List<String> tableNames = this.jdbcTemplate.queryForList(
			"""
				select table_name
				from information_schema.tables
				where table_schema = 'public'
				order by table_name
				""",
			String.class
		);

		assertThat(tableNames).contains(
			"tb_despesas",
			"households",
			"users",
			"household_members",
			"categories",
			"subcategories",
			"space_references",
			"expenses",
			"payments"
		);
	}

	@Test
	void deveCriarIndicesPrincipaisParaConsultasFinanceiras() {
		List<String> indexNames = this.jdbcTemplate.queryForList(
			"""
				select indexname
				from pg_indexes
				where schemaname = 'public'
				order by indexname
				""",
			String.class
		);

		assertThat(indexNames).contains(
			"idx_categories_household_id_name",
			"idx_expenses_household_id_due_date_id",
			"idx_expenses_household_id_category_snapshot_due_date_id",
			"idx_expenses_household_id_category_fk_due_date_id",
			"idx_expenses_household_id_subcategory_fk_due_date_id",
			"idx_payments_expense_id_paid_at_id",
			"idx_space_references_household_normalized_name",
			"idx_space_references_household_type_name",
			"idx_subcategories_category_id_name",
			"idx_subcategories_household_id_category_id_name",
			"uq_expenses_legacy_tb_despesa_id",
			"uq_household_members_user_active",
			"uq_space_references_household_type_normalized_active"
		);
	}

	@Test
	void deveFormalizarColunasDeSnapshotHistoricoNaTabelaExpenses() {
		List<String> columnNames = this.jdbcTemplate.queryForList(
			"""
				select column_name
				from information_schema.columns
				where table_schema = 'public'
				  and table_name = 'expenses'
				order by column_name
				""",
			String.class
		);

		assertThat(columnNames).contains("category_name_snapshot", "subcategory_name_snapshot", "legacy_tb_despesa_id");
	}

	@Test
	void deve_adicionar_colunas_minimas_de_onboarding_no_usuario() {
		List<String> columnNames = this.jdbcTemplate.queryForList(
			"""
				select column_name
				from information_schema.columns
				where table_schema = 'public'
				  and table_name = 'users'
				order by column_name
				""",
			String.class
		);

		assertThat(columnNames).contains("onboarding_completed", "onboarding_completed_at");
	}

	@Test
	void deve_criar_tabela_de_referencias_do_espaco_com_colunas_minimas() {
		List<String> columnNames = this.jdbcTemplate.queryForList(
			"""
				select column_name
				from information_schema.columns
				where table_schema = 'public'
				  and table_name = 'space_references'
				order by column_name
				""",
			String.class
		);

		assertThat(columnNames).contains(
			"household_id",
			"type",
			"name",
			"normalized_name",
			"created_at",
			"updated_at",
			"deleted_at"
		);
	}

	@Test
	void deveImpedirSubcategoriaComCategoriaDeOutroHousehold() {
		this.jdbcTemplate.update("insert into households (name) values (?)", "Casa A");
		this.jdbcTemplate.update("insert into households (name) values (?)", "Casa B");

		Long householdA = this.jdbcTemplate.queryForObject("select max(id) - 1 from households", Long.class);
		Long householdB = this.jdbcTemplate.queryForObject("select max(id) from households", Long.class);

		this.jdbcTemplate.update(
			"insert into categories (household_id, name, active) values (?, ?, true)",
			householdA,
			"Moradia"
		);

		Long categoryA = this.jdbcTemplate.queryForObject("select max(id) from categories", Long.class);

		assertThatThrownBy(() -> this.jdbcTemplate.update(
			"insert into subcategories (household_id, category_id, name, active) values (?, ?, ?, true)",
			householdB,
			categoryA,
			"Invalida"
		)).isInstanceOf(Exception.class);
	}

	private static void createDatabase() {
		try (Connection connection = DriverManager.getConnection(ADMIN_DB_URL, DB_USERNAME, DB_PASSWORD);
		     Statement statement = connection.createStatement()) {
			statement.execute("drop database if exists " + DB_NAME);
			statement.execute("create database " + DB_NAME);
		} catch (SQLException exception) {
			throw new IllegalStateException("Could not create temporary Flyway test database", exception);
		}
	}
}
