package com.gilrossi.despesas.api.v1.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ForgotPasswordResponse(String maskedEmail, String resetToken) {
}
