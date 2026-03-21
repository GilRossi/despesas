package com.gilrossi.despesas.emailingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gilrossi.despesas.security.CurrentHouseholdProvider;

@ExtendWith(MockitoExtension.class)
class EmailIngestionSourceServiceTest {

	@Mock
	private EmailIngestionSourceRepository repository;

	@Mock
	private CurrentHouseholdProvider currentHouseholdProvider;

	private EmailIngestionSourceService service;

	@BeforeEach
	void setUp() {
		service = new EmailIngestionSourceService(repository, currentHouseholdProvider);
	}

	@Test
	void deve_registrar_source_com_thresholds_padrao_quando_nao_informados() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(42L);
		when(repository.findByNormalizedSourceAccount("financeiro@gmail.com")).thenReturn(Optional.empty());
		when(repository.save(any())).thenAnswer(invocation -> {
			EmailIngestionSource source = invocation.getArgument(0);
			return new EmailIngestionSource(
				10L,
				source.householdId(),
				source.sourceAccount(),
				source.normalizedSourceAccount(),
				source.label(),
				source.active(),
				source.autoImportMinConfidence(),
				source.reviewMinConfidence(),
				OffsetDateTime.parse("2026-03-20T10:15:30Z"),
				OffsetDateTime.parse("2026-03-20T10:15:30Z")
			);
		});

		EmailIngestionSource source = service.register(new RegisterEmailIngestionSourceCommand("financeiro@gmail.com", "Gmail pessoal", null, null));

		assertThat(source.autoImportMinConfidence()).isEqualByComparingTo("0.90");
		assertThat(source.reviewMinConfidence()).isEqualByComparingTo("0.65");
		verify(repository).findByNormalizedSourceAccount("financeiro@gmail.com");
		verify(repository).save(any());
	}

	@Test
	void deve_rejeitar_thresholds_inconsistentes() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(42L);

		assertThatThrownBy(() -> service.register(new RegisterEmailIngestionSourceCommand(
			"financeiro@gmail.com",
			null,
			new BigDecimal("0.60"),
			new BigDecimal("0.70")
		)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("autoImportMinConfidence must be greater than or equal to reviewMinConfidence");
	}

	@Test
	void deve_rejeitar_source_duplicado() {
		when(currentHouseholdProvider.requireHouseholdId()).thenReturn(42L);
		when(repository.findByNormalizedSourceAccount("financeiro@gmail.com")).thenReturn(Optional.of(new EmailIngestionSource(
			99L,
			42L,
			"financeiro@gmail.com",
			"financeiro@gmail.com",
			null,
			true,
			new BigDecimal("0.90"),
			new BigDecimal("0.65"),
			OffsetDateTime.now(),
			OffsetDateTime.now()
		)));

		assertThatThrownBy(() -> service.register(new RegisterEmailIngestionSourceCommand("financeiro@gmail.com", null, null, null)))
			.isInstanceOf(DuplicateEmailIngestionSourceException.class)
			.hasMessage("Source account is already mapped: financeiro@gmail.com");

		verify(repository).findByNormalizedSourceAccount(eq("financeiro@gmail.com"));
	}
}
