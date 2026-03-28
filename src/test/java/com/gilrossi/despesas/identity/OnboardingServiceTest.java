package com.gilrossi.despesas.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

	@Mock
	private AppUserRepository appUserRepository;

	private OnboardingService service;
	private Clock clock;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2026-03-28T12:30:00Z"), ZoneOffset.UTC);
		service = new OnboardingService(appUserRepository, clock);
	}

	@Test
	void deve_marcar_onboarding_como_concluido_na_primeira_chamada() {
		AppUser user = new AppUser("Ana", "ana@local.invalid", "{bcrypt}hash");
		user.setId(1L);
		when(appUserRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

		OnboardingStatusResponse response = service.complete(1L);

		assertThat(response.completed()).isTrue();
		assertThat(response.completedAt()).isEqualTo(Instant.parse("2026-03-28T12:30:00Z"));
		assertThat(user.isOnboardingCompleted()).isTrue();
		assertThat(user.getOnboardingCompletedAt()).isEqualTo(Instant.parse("2026-03-28T12:30:00Z"));
	}

	@Test
	void deve_ser_idempotente_ao_concluir_onboarding_repetidamente() {
		AppUser user = new AppUser("Ana", "ana@local.invalid", "{bcrypt}hash");
		user.setId(1L);
		user.markOnboardingCompleted(Instant.parse("2026-03-20T09:00:00Z"));
		when(appUserRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

		OnboardingStatusResponse response = service.complete(1L);

		assertThat(response.completed()).isTrue();
		assertThat(response.completedAt()).isEqualTo(Instant.parse("2026-03-20T09:00:00Z"));
		assertThat(user.getOnboardingCompletedAt()).isEqualTo(Instant.parse("2026-03-20T09:00:00Z"));
	}

	@Test
	void deve_retornar_estado_atual_do_onboarding() {
		AppUser user = new AppUser("Ana", "ana@local.invalid", "{bcrypt}hash");
		user.setId(1L);
		when(appUserRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

		OnboardingStatusResponse response = service.currentStatus(1L);

		assertThat(response.completed()).isFalse();
		assertThat(response.completedAt()).isNull();
	}

	@Test
	void deve_falhar_quando_usuario_nao_existe() {
		when(appUserRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.complete(1L))
			.isInstanceOf(AppUserNotFoundException.class)
			.hasMessage("User not found");
		verify(appUserRepository).findByIdAndDeletedAtIsNull(1L);
	}
}
