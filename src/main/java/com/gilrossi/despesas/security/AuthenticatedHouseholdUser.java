package com.gilrossi.despesas.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.gilrossi.despesas.identity.AppUser;
import com.gilrossi.despesas.identity.HouseholdMember;
import com.gilrossi.despesas.identity.HouseholdMemberRole;

public class AuthenticatedHouseholdUser implements UserDetails {

	private final Long userId;
	private final Long householdId;
	private final String role;
	private final String name;
	private final String username;
	private final String password;
	private final List<GrantedAuthority> authorities;

	public AuthenticatedHouseholdUser(Long userId, Long householdId, String role, String name, String username, String password) {
		this.userId = userId;
		this.householdId = householdId;
		this.role = role;
		this.name = name;
		this.username = username;
		this.password = password;
		this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}

	public AuthenticatedHouseholdUser(Long userId, Long householdId, HouseholdMemberRole role, String name, String username, String password) {
		this(userId, householdId, role.name(), name, username, password);
	}

	public static AuthenticatedHouseholdUser from(AppUser user, HouseholdMember member) {
		return new AuthenticatedHouseholdUser(
			user.getId(),
			member.getHouseholdId(),
			member.getRole().name(),
			user.getName(),
			user.getEmail(),
			user.getPasswordHash()
		);
	}

	public static AuthenticatedHouseholdUser platformAdmin(AppUser user) {
		return new AuthenticatedHouseholdUser(
			user.getId(),
			null,
			"PLATFORM_ADMIN",
			user.getName(),
			user.getEmail(),
			user.getPasswordHash()
		);
	}

	public Long getUserId() {
		return userId;
	}

	public Long getHouseholdId() {
		return householdId;
	}

	public String getRole() {
		return role;
	}

	public String getDisplayName() {
		return name;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
