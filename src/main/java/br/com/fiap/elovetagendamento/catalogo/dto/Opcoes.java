package br.com.fiap.elovetagendamento.catalogo.dto;

/** Itens usados para preencher os campos de seleção do portal. */
public final class Opcoes {

    private Opcoes() {
    }

    public record Clinica(Long id, String nome) {
    }

    public record Veterinario(Long id, String nome, String crmv) {
    }

    public record Responsavel(Long id, String nome) {
    }

    public record Pet(Long id, String nome, String especie, String raca) {
    }
}
