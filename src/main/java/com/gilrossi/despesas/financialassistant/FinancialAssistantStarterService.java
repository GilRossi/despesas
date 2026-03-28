package com.gilrossi.despesas.financialassistant;

import org.springframework.stereotype.Service;

@Service
public class FinancialAssistantStarterService {

	public FinancialAssistantStarterResponse respond(FinancialAssistantStarterIntent intent) {
		return switch (intent) {
			case FIXED_BILLS -> FinancialAssistantStarterResponse.starter(
				intent,
				"Vamos começar pelas suas contas fixas",
				"Eu posso te guiar para registrar aluguel, internet, energia e outras despesas que se repetem.",
				"OPEN_FIXED_BILLS"
			);
			case IMPORT_HISTORY -> FinancialAssistantStarterResponse.starter(
				intent,
				"Vamos trazer seu historico",
				"Se voce ja organiza sua vida financeira em outro lugar, eu posso te orientar no primeiro passo para trazer esse historico.",
				"OPEN_IMPORT_HISTORY"
			);
			case REGISTER_INCOME -> FinancialAssistantStarterResponse.starter(
				intent,
				"Vamos registrar seus ganhos",
				"Comecar pelos seus ganhos deixa seu Espaco mais completo para acompanhar entradas e saidas desde o inicio.",
				"OPEN_REGISTER_INCOME"
			);
			case CONFIGURE_SPACE -> FinancialAssistantStarterResponse.starter(
				intent,
				"Vamos configurar seu Espaco",
				"Eu posso te ajudar a ajustar o seu Espaco para refletir a sua rotina antes dos proximos lancamentos.",
				"OPEN_CONFIGURE_SPACE"
			);
		};
	}
}
