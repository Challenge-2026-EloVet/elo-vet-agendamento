package br.com.fiap.elovetagendamento.auditoria.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SincronizacaoResponse(
        LocalDate dataReferencia,
        int documentosGravados,
        long documentosSubstituidos,
        LocalDateTime executadoEm
) {
}
