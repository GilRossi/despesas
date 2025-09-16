package com.gilrossi.despesas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.gilrossi.despesas.model.Despesa;
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
	public String listar(Model model) {
		model.addAttribute("despesas", service.listarDespesas());
		return "despesas/lista"; // lista.html
	}

	@GetMapping("/nova")
	public String nova(Model model) {
		model.addAttribute("despesa", new Despesa());
		return "despesas/form"; // form.html
	}

	@PostMapping("/salvar")
	public String salvar(@Valid @ModelAttribute("despesa") Despesa despesa, BindingResult result, Model model) {
		if (result.hasErrors()) {
			return "despesas/form";
		}
		service.salvar(despesa);
		return "redirect:/despesas";
	}

	@GetMapping("/deletar/{id}")
	public String deletar(@PathVariable Long id) {
		service.deletar(id);
		return "redirect:/despesas";
	}
}
