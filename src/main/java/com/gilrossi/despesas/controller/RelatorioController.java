package com.gilrossi.despesas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.gilrossi.despesas.service.RelatorioService;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

	private final RelatorioService service;

	public RelatorioController(RelatorioService service) {
		this.service = service;
	}

	@GetMapping
	public String exibir(
		@RequestParam(required = false) String referenceMonth,
		@RequestParam(defaultValue = "true") boolean comparePrevious,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		try {
			model.addAttribute("relatorio", service.carregarPagina(referenceMonth, comparePrevious));
			return "despesas/relatorios";
		} catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
			return "redirect:/relatorios";
		}
	}

	@PostMapping("/assistente")
	public String consultarAssistente(
		@RequestParam String action,
		@RequestParam(required = false) String referenceMonth,
		@RequestParam(defaultValue = "true") boolean comparePrevious,
		RedirectAttributes redirectAttributes
	) {
		try {
			redirectAttributes.addFlashAttribute("assistantResponse", service.executarAtalho(action, referenceMonth));
		} catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
		}
		return "redirect:" + UriComponentsBuilder.fromPath("/relatorios")
			.queryParamIfPresent("referenceMonth", java.util.Optional.ofNullable(referenceMonth).filter(value -> !value.isBlank()))
			.queryParam("comparePrevious", comparePrevious)
			.build()
			.toUriString();
	}
}
