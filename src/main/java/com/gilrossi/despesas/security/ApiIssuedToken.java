package com.gilrossi.despesas.security;

import java.time.Instant;

public record ApiIssuedToken(String value, Instant expiresAt) {
}
