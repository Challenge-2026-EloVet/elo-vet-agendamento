package br.com.fiap.elovetagendamento.agendamento.dto;

import br.com.fiap.elovetagendamento.agendamento.model.StatusAgendamento;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AgendamentoRequest(
        @NotNull(message = "O pet é obrigatório")
        Long idPet,

        @NotNull(message = "O responsável é obrigatório")
        Long idResponsavel,

        @NotNull(message = "O veterinário é obrigatório")
        Long idVeterinario,

        @NotNull(message = "A clínica é obrigatória")
        Long idClinica,

        @NotNull(message = "A data e hora do agendamento são obrigatórias")
        @Future(message = "O agendamento deve ser para uma data futura")
        LocalDateTime dataHora,

        // Opcional: na criação, o padrão é AGENDADO
        StatusAgendamento status,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao
) {
}
