package br.com.fiap.elovetagendamento.auditoria.controller;

import br.com.fiap.elovetagendamento.auditoria.document.AuditoriaDocument;
import br.com.fiap.elovetagendamento.auditoria.dto.SincronizacaoResponse;
import br.com.fiap.elovetagendamento.auditoria.service.AuditoriaSincronizacaoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auditorias")
public class AuditoriaController {

    private final AuditoriaSincronizacaoService sincronizacaoService;

    public AuditoriaController(AuditoriaSincronizacaoService sincronizacaoService) {
        this.sincronizacaoService = sincronizacaoService;
    }

    /** Dispara na hora a mesma sincronização que o agendador executa diariamente. */
    @PostMapping("/sincronizacao")
    public SincronizacaoResponse sincronizar() {
        return sincronizacaoService.sincronizar();
    }

    /** Lista a auditoria já gravada no MongoDB, opcionalmente filtrando por agendamento. */
    @GetMapping
    public List<AuditoriaDocument> listar(@RequestParam(required = false) Long idAgendamento) {
        return sincronizacaoService.listar(idAgendamento);
    }
}
