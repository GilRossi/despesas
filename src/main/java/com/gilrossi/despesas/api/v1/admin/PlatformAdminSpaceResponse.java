package com.gilrossi.despesas.api.v1.admin;

import java.time.Instant;
import java.util.List;

public record PlatformAdminSpaceResponse(
	Long spaceId,
	String spaceName,
	Instant createdAt,
	Instant updatedAt,
	int activeMembersCount,
	Owner owner,
	List<Module> modules
) {

	public record Owner(
		Long userId,
		String name,
		String email
	) {
	}

	public record Module(
		String key,
		boolean enabled,
		boolean mandatory
	) {
	}
}
