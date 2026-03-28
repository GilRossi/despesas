package com.gilrossi.despesas.financialassistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FinancialAssistantStarterServiceTest {

	private final FinancialAssistantStarterService service = new FinancialAssistantStarterService();

	@ParameterizedTest
	@MethodSource("starterCases")
	void deve_retornar_payload_deterministico_para_cada_intent(
		FinancialAssistantStarterIntent intent,
		String title,
		String message,
		String primaryActionKey
	) {
		FinancialAssistantStarterResponse response = service.respond(intent);

		assertThat(response.intent()).isEqualTo(intent);
		assertThat(response.kind()).isEqualTo(FinancialAssistantStarterKind.STARTER);
		assertThat(response.title()).isEqualTo(title);
		assertThat(response.message()).isEqualTo(message);
		assertThat(response.primaryActionKey()).isEqualTo(primaryActionKey);
	}

	private static Stream<Arguments> starterCases() {
		return Stream.of(
			Arguments.of(
				FinancialAssistantStarterIntent.FIXED_BILLS,
				"Vamos começar pelas suas contas fixas",
				"Eu posso te guiar para registrar aluguel, internet, energia e outras despesas que se repetem.",
				"OPEN_FIXED_BILLS"
			),
			Arguments.of(
				FinancialAssistantStarterIntent.IMPORT_HISTORY,
				"Vamos trazer seu historico",
				"Se voce ja organiza sua vida financeira em outro lugar, eu posso te orientar no primeiro passo para trazer esse historico.",
				"OPEN_IMPORT_HISTORY"
			),
			Arguments.of(
				FinancialAssistantStarterIntent.REGISTER_INCOME,
				"Vamos registrar seus ganhos",
				"Comecar pelos seus ganhos deixa seu Espaco mais completo para acompanhar entradas e saidas desde o inicio.",
				"OPEN_REGISTER_INCOME"
			),
			Arguments.of(
				FinancialAssistantStarterIntent.CONFIGURE_SPACE,
				"Vamos configurar seu Espaco",
				"Eu posso te ajudar a ajustar o seu Espaco para refletir a sua rotina antes dos proximos lancamentos.",
				"OPEN_CONFIGURE_SPACE"
			)
		);
	}
}
