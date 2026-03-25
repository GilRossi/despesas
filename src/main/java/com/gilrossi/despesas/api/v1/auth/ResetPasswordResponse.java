package com.gilrossi.despesas.api.v1.auth;

public record ResetPasswordResponse(int revokedRefreshTokens, boolean success) {
}
