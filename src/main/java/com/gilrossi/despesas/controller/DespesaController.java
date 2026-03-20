package com.gilrossi.despesas.controller;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gilrossi.despesas.model.Despesa;
import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.service.DespesaService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/despesas")
public class DespesaController {

	private final DespesaService service;

	public DespesaController(DespesaService service) {
		this.service = service;
	}

	@GetMapping
	public String listar(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		Model model
	) {
		Page<Despesa> pagina = service.listarDespesas(page, size);
		model.addAttribute("pagina", pagina);
		model.addAttribute("despesas", pagina.getContent());
		return "despesas/lista";
	}

	@GetMapping("/nova")
	public String nova(Model model) {
		model.addAttribute("despesa", new Despesa());
		adicionarContextoDoFormulario(model);
		return "despesas/form";
	}

	@GetMapping("/editar/{id}")
	public String editar(@PathVariable Long id, Model model) {
		model.addAttribute("despesa", service.buscaPorId(id));
		adicionarContextoDoFormulario(model);
		return "despesas/form";
	}

	@PostMapping("/salvar")
	public String salvar(
		@Valid @ModelAttribute("despesa") Despesa despesa,
		BindingResult result,
		Model model,
		RedirectAttributes redirectAttributes
	) {
		if (result.hasErrors()) {
			adicionarContextoDoFormulario(model);
			return "despesas/form";
		}
		boolean emEdicao = despesa.getId() != null;
		try {
			service.salvar(despesa);
		} catch (IllegalArgumentException exception) {
			result.reject("businessRule", exception.getMessage());
			adicionarContextoDoFormulario(model);
			return "despesas/form";
		}
		redirectAttributes.addFlashAttribute(
			"mensagemSucesso",
			emEdicao ? "Despesa atualizada com sucesso." : "Despesa cadastrada com sucesso."
		);
		return "redirect:/despesas";
	}

	@PostMapping("/{id}/excluir")
	public String deletar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			service.deletar(id);
			redirectAttributes.addFlashAttribute("mensagemSucesso", "Despesa excluída com sucesso.");
		} catch (IllegalArgumentException exception) {
			redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
		}
		return "redirect:/despesas";
	}

	private void adicionarContextoDoFormulario(Model model) {
		model.addAttribute("catalogo", service.listarCatalogo());
		model.addAttribute("contextos", ExpenseContext.values());
	}
}
