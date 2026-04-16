package com.gilrossi.despesas.api.v1.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PlatformAdminPlatformHealthResponse(
	String applicationStatus,
	Instant checkedAt,
	ActuatorExposure actuator,
	DeploymentSnapshot deployment,
	RuntimeSnapshot runtime,
	JvmSnapshot jvm,
	SystemSnapshot system,
	Map<String, Object> info,
	List<OperationalAlert> alerts
) {

	public record ActuatorExposure(
		boolean healthExposed,
		boolean infoExposed,
		boolean metricsExposed
	) {
	}

	public record DeploymentSnapshot(
		String applicationName,
		String artifact,
		String version,
		Instant builtAt
	) {
	}

	public record RuntimeSnapshot(
		String livenessState,
		String readinessState,
		Instant startedAt
	) {
	}

	public record JvmSnapshot(
		long availableProcessors,
		long uptimeMs,
		long heapUsedBytes,
		long heapCommittedBytes,
		long heapMaxBytes
	) {
	}

	public record SystemSnapshot(
		Double systemLoadAverage
	) {
	}

	public record OperationalAlert(
		String code,
		String severity,
		String source,
		String title,
		String message
	) {
	}
}
