package com.gilrossi.despesas.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gilrossi.despesas.service.DespesaNotFoundException;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

	@ExceptionHandler(DespesaNotFoundException.class)
	public String handleDespesaNotFound(DespesaNotFoundException ex, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("mensagemErro", ex.getMessage());
		return "redirect:/despesas";
	}
}
