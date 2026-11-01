package br.com.fiap.elovetagendamento.agendamento.dto;

import br.com.fiap.elovetagendamento.agendamento.model.StatusAgendamento;

import java.time.LocalDateTime;

public record AgendamentoResponse(
        Long idAgendamento,
        Long idPet,
        String nomePet,
        String especiePet,
        Long idResponsavel,
        String nomeResponsavel,
        Long idVeterinario,
        String nomeVeterinario,
        Long idClinica,
        String nomeClinica,
        LocalDateTime dataHora,
        StatusAgendamento status,
        String observacao
) {
}
