package br.com.fiap.elovetagendamento.auditoria.dto;

import java.time.LocalDateTime;

public record SincronizacaoResponse(
        int registrosSincronizados,
        Long ultimoIdAuditoria,
        LocalDateTime executadoEm
) {
}
