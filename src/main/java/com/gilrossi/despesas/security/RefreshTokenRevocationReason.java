package com.gilrossi.despesas.security;

public enum RefreshTokenRevocationReason {
	ROTATED,
	LOGOUT,
	REUSE_DETECTED,
	EXPIRED,
	USER_STATE_INVALID
}
