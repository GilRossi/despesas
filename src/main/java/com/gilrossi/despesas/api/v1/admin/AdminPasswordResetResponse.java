package com.gilrossi.despesas.api.v1.admin;

public record AdminPasswordResetResponse(
	String targetEmailMasked,
	int revokedRefreshTokens
) {
}
