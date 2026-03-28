package com.gilrossi.despesas.spacereference;

import java.util.Arrays;
import java.util.List;

public enum SpaceReferenceType {
	APARTAMENTO(SpaceReferenceTypeGroup.RESIDENCIAL),
	CASA(SpaceReferenceTypeGroup.RESIDENCIAL),
	CHACARA(SpaceReferenceTypeGroup.RESIDENCIAL),
	FAZENDA(SpaceReferenceTypeGroup.RESIDENCIAL),
	SITIO(SpaceReferenceTypeGroup.RESIDENCIAL),
	TERRENO(SpaceReferenceTypeGroup.RESIDENCIAL),
	SOBRADO(SpaceReferenceTypeGroup.RESIDENCIAL),
	KITNET(SpaceReferenceTypeGroup.RESIDENCIAL),
	COBERTURA(SpaceReferenceTypeGroup.RESIDENCIAL),
	STUDIO_RESIDENCIAL(SpaceReferenceTypeGroup.RESIDENCIAL),

	ESCRITORIO(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	SALA_COMERCIAL(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	LOJA(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	GALPAO(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	ESTUDIO(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	CONSULTORIO(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	CLINICA(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	OFICINA(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	PONTO_COMERCIAL(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	COWORKING(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	DEPOSITO(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	TRABALHO(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	CLIENTE(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	PROJETO(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	CONTRATO(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	UNIDADE_ATENDIMENTO(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	FILIAL(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),
	COMERCIAL_OUTRO(SpaceReferenceTypeGroup.COMERCIAL_TRABALHO),

	CARRO(SpaceReferenceTypeGroup.VEICULOS),
	MOTO(SpaceReferenceTypeGroup.VEICULOS),
	CAMINHAO(SpaceReferenceTypeGroup.VEICULOS),
	VAN(SpaceReferenceTypeGroup.VEICULOS),
	UTILITARIO(SpaceReferenceTypeGroup.VEICULOS),

	LANCHA(SpaceReferenceTypeGroup.EMBARCACAO),
	BARCO(SpaceReferenceTypeGroup.EMBARCACAO),
	VELEIRO(SpaceReferenceTypeGroup.EMBARCACAO),
	JET_SKI(SpaceReferenceTypeGroup.EMBARCACAO),
	IATE(SpaceReferenceTypeGroup.EMBARCACAO),
	MARINA(SpaceReferenceTypeGroup.EMBARCACAO),
	EMBARCACAO_OUTRO(SpaceReferenceTypeGroup.EMBARCACAO),

	AVIAO(SpaceReferenceTypeGroup.AVIACAO),
	HELICOPTERO(SpaceReferenceTypeGroup.AVIACAO),
	HANGAR(SpaceReferenceTypeGroup.AVIACAO),
	AVIACAO_OUTRO(SpaceReferenceTypeGroup.AVIACAO);

	private final SpaceReferenceTypeGroup group;

	SpaceReferenceType(SpaceReferenceTypeGroup group) {
		this.group = group;
	}

	public SpaceReferenceTypeGroup group() {
		return group;
	}

	public static List<SpaceReferenceType> fromGroup(SpaceReferenceTypeGroup group) {
		return Arrays.stream(values())
			.filter(type -> type.group == group)
			.toList();
	}
}
