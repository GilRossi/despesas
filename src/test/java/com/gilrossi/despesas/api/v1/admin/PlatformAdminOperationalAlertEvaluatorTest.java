package com.gilrossi.despesas.api.v1.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlatformAdminOperationalAlertEvaluatorTest {

	private final PlatformAdminOperationalAlertEvaluator evaluator = new PlatformAdminOperationalAlertEvaluator();

	@Test
	void deve_sinalizar_quando_metrics_estiver_fechado_e_info_vazio() {
		var alerts = evaluator.evaluate(baseInput().toInput());

		assertThat(alerts)
			.extracting(PlatformAdminPlatformHealthResponse.OperationalAlert::code)
			.containsExactly("ACTUATOR_METRICS_NOT_EXPOSED", "ACTUATOR_INFO_EMPTY");
	}

	@Test
	void deve_sinalizar_aplicacao_degradada() {
		var alerts = evaluator.evaluate(baseInput().withApplicationStatus("DOWN").toInput());

		assertThat(alerts)
			.extracting(PlatformAdminPlatformHealthResponse.OperationalAlert::code)
			.contains("APPLICATION_STATUS_DEGRADED");
	}

	@Test
	void deve_sinalizar_heap_critico() {
		var alerts = evaluator.evaluate(baseInput().withHeapUsedBytes(950).withHeapMaxBytes(1000).toInput());

		assertThat(alerts)
			.extracting(PlatformAdminPlatformHealthResponse.OperationalAlert::code)
			.contains("HEAP_USAGE_CRITICAL");
	}

	@Test
	void deve_sinalizar_load_alto_quando_ultrapassar_processadores() {
		var alerts = evaluator.evaluate(baseInput().withSystemLoadAverage(4.2).toInput());

		assertThat(alerts)
			.extracting(PlatformAdminPlatformHealthResponse.OperationalAlert::code)
			.contains("SYSTEM_LOAD_HIGH");
	}

	@Test
	void deve_sinalizar_load_indisponivel_quando_fonte_nao_existe() {
		var alerts = evaluator.evaluate(baseInput().withSystemLoadAverage(null).toInput());

		assertThat(alerts)
			.extracting(PlatformAdminPlatformHealthResponse.OperationalAlert::code)
			.contains("SYSTEM_LOAD_UNAVAILABLE");
	}

	@Test
	void deve_sinalizar_espacos_sem_responsavel() {
		var alerts = evaluator.evaluate(baseInput().withSpacesWithoutOwner(2).toInput());

		assertThat(alerts)
			.extracting(PlatformAdminPlatformHealthResponse.OperationalAlert::code)
			.contains("SPACES_WITHOUT_OWNER");
	}

	private TestInput baseInput() {
		return new TestInput(
			"UP",
			true,
			true,
			false,
			true,
			4,
			400,
			1000,
			0.25,
			0
		);
	}

	private record TestInput(
		String applicationStatus,
		boolean healthExposed,
		boolean infoExposed,
		boolean metricsExposed,
		boolean infoEmpty,
		int availableProcessors,
		long heapUsedBytes,
		long heapMaxBytes,
		Double systemLoadAverage,
		long spacesWithoutOwner
	) {
		PlatformAdminOperationalAlertEvaluator.Input toInput() {
			return new PlatformAdminOperationalAlertEvaluator.Input(
				applicationStatus,
				healthExposed,
				infoExposed,
				metricsExposed,
				infoEmpty,
				availableProcessors,
				heapUsedBytes,
				heapMaxBytes,
				systemLoadAverage,
				spacesWithoutOwner
			);
		}

		TestInput withApplicationStatus(String value) {
			return new TestInput(
				value,
				healthExposed,
				infoExposed,
				metricsExposed,
				infoEmpty,
				availableProcessors,
				heapUsedBytes,
				heapMaxBytes,
				systemLoadAverage,
				spacesWithoutOwner
			);
		}

		TestInput withHeapUsedBytes(long value) {
			return new TestInput(
				applicationStatus,
				healthExposed,
				infoExposed,
				metricsExposed,
				infoEmpty,
				availableProcessors,
				value,
				heapMaxBytes,
				systemLoadAverage,
				spacesWithoutOwner
			);
		}

		TestInput withHeapMaxBytes(long value) {
			return new TestInput(
				applicationStatus,
				healthExposed,
				infoExposed,
				metricsExposed,
				infoEmpty,
				availableProcessors,
				heapUsedBytes,
				value,
				systemLoadAverage,
				spacesWithoutOwner
			);
		}

		TestInput withSystemLoadAverage(Double value) {
			return new TestInput(
				applicationStatus,
				healthExposed,
				infoExposed,
				metricsExposed,
				infoEmpty,
				availableProcessors,
				heapUsedBytes,
				heapMaxBytes,
				value,
				spacesWithoutOwner
			);
		}

		TestInput withSpacesWithoutOwner(long value) {
			return new TestInput(
				applicationStatus,
				healthExposed,
				infoExposed,
				metricsExposed,
				infoEmpty,
				availableProcessors,
				heapUsedBytes,
				heapMaxBytes,
				systemLoadAverage,
				value
			);
		}
	}

}
