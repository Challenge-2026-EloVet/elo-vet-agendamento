package br.com.fiap.elovetagendamento.catalogo.dto;

import java.util.Map;

/** Nomes indexados por id, usados para descrever um agendamento sem um SELECT por linha. */
public record NomesCatalogo(
        Map<Long, String> pets,
        Map<Long, String> especies,
        Map<Long, String> responsaveis,
        Map<Long, String> veterinarios,
        Map<Long, String> clinicas
) {
}
