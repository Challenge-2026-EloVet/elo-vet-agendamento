package br.com.fiap.elovetagendamento.auditoria.controller;

import br.com.fiap.elovetagendamento.auditoria.dto.SincronizacaoResponse;
import br.com.fiap.elovetagendamento.auditoria.service.AuditoriaSincronizacaoService;
import org.bson.Document;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/auditorias")
public class AuditoriaController {

    private final AuditoriaSincronizacaoService sincronizacaoService;

    public AuditoriaController(AuditoriaSincronizacaoService sincronizacaoService) {
        this.sincronizacaoService = sincronizacaoService;
    }

    /** Executa na hora a mesma exportação que o agendador roda diariamente. */
    @PostMapping("/sincronizacao")
    public SincronizacaoResponse sincronizar() {
        return sincronizacaoService.sincronizar();
    }

    /**
     * Lista os documentos gravados no MongoDB, exatamente como estão na collection.
     * Filtros opcionais: idAgendamento e dataReferencia (formato AAAA-MM-DD).
     */
    @GetMapping
    public List<Document> listar(
            @RequestParam(required = false) Long idAgendamento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia) {
        return sincronizacaoService.listar(idAgendamento, dataReferencia);
    }
}
