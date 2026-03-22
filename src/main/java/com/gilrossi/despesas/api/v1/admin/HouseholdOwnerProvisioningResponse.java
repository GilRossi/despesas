package com.gilrossi.despesas.api.v1.admin;

public record HouseholdOwnerProvisioningResponse(
	Long householdId,
	String householdName,
	Long ownerUserId,
	String ownerEmail,
	String ownerRole
) {
}
