package com.gilrossi.despesas.api.v1.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class PlatformAdminOperationalAlertEvaluator {

	private static final double HEAP_WARNING_RATIO = 0.75d;
	private static final double HEAP_CRITICAL_RATIO = 0.90d;
	private static final double LOAD_WARNING_RATIO = 1.0d;
	private static final double LOAD_CRITICAL_RATIO = 1.5d;

	public List<PlatformAdminPlatformHealthResponse.OperationalAlert> evaluate(Input input) {
		List<PlatformAdminPlatformHealthResponse.OperationalAlert> alerts = new ArrayList<>();

		if (!"UP".equalsIgnoreCase(input.applicationStatus())) {
			alerts.add(alert(
				"APPLICATION_STATUS_DEGRADED",
				"CRITICAL",
				"APPLICATION",
				"Aplicação degradada",
				"Status atual da aplicação: %s.".formatted(input.applicationStatus())
			));
		}

		if (!input.healthExposed()) {
			alerts.add(alert(
				"ACTUATOR_HEALTH_NOT_EXPOSED",
				"WARNING",
				"ACTUATOR",
				"Actuator health não exposto",
				"O endpoint de health não está exposto por HTTP para monitoramento externo."
			));
		}

		if (!input.metricsExposed()) {
			alerts.add(alert(
				"ACTUATOR_METRICS_NOT_EXPOSED",
				"WARNING",
				"ACTUATOR",
				"Actuator metrics fechado",
				"As métricas do Actuator ainda não estão expostas por HTTP nesta fase."
			));
		}

		if (!input.infoExposed()) {
			alerts.add(alert(
				"ACTUATOR_INFO_NOT_EXPOSED",
				"INFO",
				"ACTUATOR",
				"Actuator info não exposto",
				"O endpoint de info não está exposto por HTTP nesta fase."
			));
		} else if (input.infoEmpty()) {
			alerts.add(alert(
				"ACTUATOR_INFO_EMPTY",
				"INFO",
				"ACTUATOR",
				"Actuator info vazio",
				"O endpoint de info está exposto, mas sem dados extras publicados agora."
			));
		}

		if (input.heapMaxBytes() <= 0) {
			alerts.add(alert(
				"HEAP_LIMIT_UNAVAILABLE",
				"INFO",
				"RUNTIME",
				"Limite do heap indisponível",
				"A JVM não informou o limite máximo do heap na fonte atual."
			));
		} else {
			double heapRatio = (double) input.heapUsedBytes() / (double) input.heapMaxBytes();
			if (heapRatio >= HEAP_CRITICAL_RATIO) {
				alerts.add(alert(
					"HEAP_USAGE_CRITICAL",
					"CRITICAL",
					"RUNTIME",
					"Heap em nível crítico",
					"Uso atual do heap em %s do limite configurado.".formatted(formatPercent(heapRatio))
				));
			} else if (heapRatio >= HEAP_WARNING_RATIO) {
				alerts.add(alert(
					"HEAP_USAGE_HIGH",
					"WARNING",
					"RUNTIME",
					"Heap em nível de atenção",
					"Uso atual do heap em %s do limite configurado.".formatted(formatPercent(heapRatio))
				));
			}
		}

		if (input.systemLoadAverage() == null) {
			alerts.add(alert(
				"SYSTEM_LOAD_UNAVAILABLE",
				"INFO",
				"RUNTIME",
				"Load do host indisponível",
				"A leitura de load average não está disponível na fonte atual."
			));
		} else if (input.availableProcessors() > 0) {
			double loadRatio = input.systemLoadAverage() / input.availableProcessors();
			if (loadRatio >= LOAD_CRITICAL_RATIO) {
				alerts.add(alert(
					"SYSTEM_LOAD_CRITICAL",
					"CRITICAL",
					"RUNTIME",
					"Load do host em nível crítico",
					"Load médio em %s por processador disponível.".formatted(formatRatio(loadRatio))
				));
			} else if (loadRatio >= LOAD_WARNING_RATIO) {
				alerts.add(alert(
					"SYSTEM_LOAD_HIGH",
					"WARNING",
					"RUNTIME",
					"Load do host em nível de atenção",
					"Load médio em %s por processador disponível.".formatted(formatRatio(loadRatio))
				));
			}
		}

		if (input.spacesWithoutOwner() > 0) {
			alerts.add(alert(
				"SPACES_WITHOUT_OWNER",
				"WARNING",
				"SPACES",
				"Espaços sem responsável",
				"%s Espaço(s) ainda estão sem responsável ativo definido.".formatted(input.spacesWithoutOwner())
			));
		}

		return List.copyOf(alerts);
	}

	private PlatformAdminPlatformHealthResponse.OperationalAlert alert(
		String code,
		String severity,
		String source,
		String title,
		String message
	) {
		return new PlatformAdminPlatformHealthResponse.OperationalAlert(
			code,
			severity,
			source,
			title,
			message
		);
	}

	private String formatPercent(double ratio) {
		return String.format(Locale.US, "%.1f%%", ratio * 100.0d).replace('.', ',');
	}

	private String formatRatio(double ratio) {
		return String.format(Locale.US, "%.2fx", ratio).replace('.', ',');
	}

	public record Input(
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
	}
}
