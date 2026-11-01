package br.com.fiap.elovetagendamento.agendamento.dto;

import br.com.fiap.elovetagendamento.agendamento.model.StatusAgendamento;

import java.time.LocalDateTime;

public record AgendamentoResponse(
        Long idAgendamento,
        Long idPet,
        Long idResponsavel,
        Long idVeterinario,
        Long idClinica,
        LocalDateTime dataHora,
        StatusAgendamento status,
        String observacao
) {
}
