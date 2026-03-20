package com.gilrossi.despesas.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gilrossi.despesas.expense.ExpenseContext;
import com.gilrossi.despesas.expense.ExpenseStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class Despesa {

	private Long id;

	@NotBlank(message = "A descrição não pode estar vazia")
	@Size(max = 140, message = "A descrição pode ter no máximo 140 caracteres")
	private String descricao;

	@NotNull(message = "O valor é obrigatório")
	@Positive(message = "O valor deve ser maior que 0")
	private BigDecimal valor;

	@NotNull(message = "A data é obrigatória")
	private LocalDate data;

	@NotNull(message = "O contexto é obrigatório")
	private ExpenseContext contexto;

	@NotNull(message = "A categoria é obrigatória")
	private Long categoriaId;

	private String categoria;

	@NotNull(message = "A subcategoria é obrigatória")
	private Long subcategoriaId;

	private String subcategoria;

	@Size(max = 255, message = "As observações podem ter no máximo 255 caracteres")
	private String observacoes;

	private ExpenseStatus status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public LocalDate getData() {
		return data;
	}

	public void setData(LocalDate data) {
		this.data = data;
	}

	public ExpenseContext getContexto() {
		return contexto;
	}

	public void setContexto(ExpenseContext contexto) {
		this.contexto = contexto;
	}

	public Long getCategoriaId() {
		return categoriaId;
	}

	public void setCategoriaId(Long categoriaId) {
		this.categoriaId = categoriaId;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	public Long getSubcategoriaId() {
		return subcategoriaId;
	}

	public void setSubcategoriaId(Long subcategoriaId) {
		this.subcategoriaId = subcategoriaId;
	}

	public String getSubcategoria() {
		return subcategoria;
	}

	public void setSubcategoria(String subcategoria) {
		this.subcategoria = subcategoria;
	}

	public String getObservacoes() {
		return observacoes;
	}

	public void setObservacoes(String observacoes) {
		this.observacoes = observacoes;
	}

	public ExpenseStatus getStatus() {
		return status;
	}

	public void setStatus(ExpenseStatus status) {
		this.status = status;
	}
}
