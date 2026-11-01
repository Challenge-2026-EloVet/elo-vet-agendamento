package br.com.fiap.elovetagendamento.agendamento.dto;

import br.com.fiap.elovetagendamento.agendamento.model.StatusAgendamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusRequest(
        @NotNull(message = "O status é obrigatório")
        StatusAgendamento status,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao
) {
}
