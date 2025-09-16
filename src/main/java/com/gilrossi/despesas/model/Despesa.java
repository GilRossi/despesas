package com.gilrossi.despesas.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 
 */
@Entity
@Table(name = "TB_DESPESAS")
public class Despesa {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@NotBlank(message = "A descrição não pode estar vazia")
	@Size(max = 100, message = "A descrição pode ter no máximo 100 caracteres")
	private String descricao;
	
	@NotNull(message = "O valor é obrigatório")
	@Positive(message = "O valor deve ser maior que 0, não pode ser negativo")
	private Double valor;
	
	@NotNull(message = "A data é obrigatório")
	@PastOrPresent(message = "A data não pode estar no futuro")
	private LocalDate data;
	
	@NotBlank(message = "A categoria é obrigatória")
	private String categoria;
	
	public Despesa() {
		
	}
	
	public Despesa(String descricao, Double valor, LocalDate data, String categoria) {
		this.descricao = descricao;
		this.valor = valor;
		this.data = data;
		this.categoria = categoria;		
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Double getValor() {
		return valor;
	}

	public void setValor(Double valor) {
		this.valor = valor;
	}

	public LocalDate getData() {
		return data;
	}

	public void setData(LocalDate data) {
		this.data = data;
	}

	public String getCategoria() {
		return categoria;
	}

	public void setCategoria(String categoria) {
		this.categoria = categoria;
	}

	
	
}
