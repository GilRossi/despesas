package com.gilrossi.despesas.api.v1.auth;

public record ChangePasswordResponse(
	int revokedRefreshTokens,
	boolean reauthenticationRequired
) {
}
