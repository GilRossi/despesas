package com.gilrossi.despesas.security;

public record RefreshTokenRotationResult(
	AuthenticatedHouseholdUser principal,
	ApiIssuedToken refreshToken
) {
}
