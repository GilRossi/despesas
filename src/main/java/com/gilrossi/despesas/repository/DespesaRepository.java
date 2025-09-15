package com.gilrossi.despesas.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gilrossi.despesas.model.Despesa;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long>{

}
