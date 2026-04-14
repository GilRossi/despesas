package com.gilrossi.despesas.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

import com.gilrossi.despesas.identity.HouseholdModuleKey;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@householdModuleAuthorization.canAccess(T(com.gilrossi.despesas.identity.HouseholdModuleKey).DRIVER)")
public @interface RequireDriverModule {
}
