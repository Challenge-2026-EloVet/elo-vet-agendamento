package br.com.fiap.elovetagendamento.clinica.repository;

/** Linha de elo_clinica com o endereço (elo_endereco) já unido. */
public record ClinicaRow(
        Long id,
        String nome,
        String telefone,
        Double avaliacao,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        Double latitude,
        Double longitude
) {
}
