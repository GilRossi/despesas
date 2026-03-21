package com.gilrossi.despesas.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gilrossi.despesas.emailingestion.EmailIngestionReviewActionNotAllowedException;
import com.gilrossi.despesas.emailingestion.EmailIngestionReviewNotFoundException;
import com.gilrossi.despesas.service.RevisaoService;

@Controller
@RequestMapping("/revisoes")
@PreAuthorize("hasRole('OWNER')")
public class RevisaoController {

	private final RevisaoService service;

	public RevisaoController(RevisaoService service) {
		this.service = service;
	}

	@GetMapping
	public String listar(Model model) {
		model.addAttribute("revisaoPagina", service.carregarPagina());
		return "despesas/revisoes";
	}

	@PostMapping("/{ingestionId}/aprovar")
	public String aprovar(@PathVariable Long ingestionId, RedirectAttributes redirectAttributes) {
		try {
			redirectAttributes.addFlashAttribute("mensagemSucesso", service.aprovar(ingestionId));
		} catch (EmailIngestionReviewNotFoundException | EmailIngestionReviewActionNotAllowedException exception) {
			redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
		}
		return "redirect:/revisoes";
	}

	@PostMapping("/{ingestionId}/rejeitar")
	public String rejeitar(@PathVariable Long ingestionId, RedirectAttributes redirectAttributes) {
		try {
			redirectAttributes.addFlashAttribute("mensagemSucesso", service.rejeitar(ingestionId));
		} catch (EmailIngestionReviewNotFoundException | EmailIngestionReviewActionNotAllowedException exception) {
			redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
		}
		return "redirect:/revisoes";
	}
}
