package br.com.fiap.elovetagendamento.auditoria.scheduler;

import br.com.fiap.elovetagendamento.auditoria.service.AuditoriaSincronizacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Executa a sincronização da auditoria uma vez por dia.
 * O horário é configurável pela propriedade elovet.auditoria.sync-cron.
 */
@Component
public class AuditoriaScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaScheduler.class);

    private final AuditoriaSincronizacaoService sincronizacaoService;

    public AuditoriaScheduler(AuditoriaSincronizacaoService sincronizacaoService) {
        this.sincronizacaoService = sincronizacaoService;
    }

    @Scheduled(cron = "${elovet.auditoria.sync-cron}", zone = "America/Sao_Paulo")
    public void sincronizarDiariamente() {
        log.info("Disparando exportação diária da auditoria (Oracle -> MongoDB)");
        try {
            sincronizacaoService.sincronizar();
        } catch (RuntimeException exception) {
            // Uma falha não pode derrubar o agendador; a próxima execução retoma de onde parou
            log.error("Falha na exportação diária da auditoria", exception);
        }
    }
}
