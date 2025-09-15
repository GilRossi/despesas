package com.gilrossi.despesas.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gilrossi.despesas.model.Despesa;
import com.gilrossi.despesas.repository.DespesaRepository;

@Service
public class DespesaService {
	
	private final DespesaRepository repository;
	
	public DespesaService(DespesaRepository repository) {
		this.repository = repository;
	}
	
	public List<Despesa> listarDespesas(){
		return repository.findAll();
	}
	
	public Optional<Despesa> buscaPorId(Long id){
		return repository.findById(id);
	}
	
	public Despesa salvar(Despesa despesa) {
		return repository.save(despesa);
	}
	
	public void deletar(Long id) {
		repository.deleteById(id);
	}

}
