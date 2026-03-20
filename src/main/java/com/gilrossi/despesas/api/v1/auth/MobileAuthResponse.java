package com.gilrossi.despesas.api.v1.auth;

import java.time.Instant;

import com.gilrossi.despesas.identity.AuthResponse;

public record MobileAuthResponse(
	String tokenType,
	String accessToken,
	Instant accessTokenExpiresAt,
	String refreshToken,
	Instant refreshTokenExpiresAt,
	AuthResponse user
) {
}
