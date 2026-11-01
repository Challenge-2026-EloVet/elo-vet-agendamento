package br.com.fiap.elovetagendamento.auditoria.service;

import br.com.fiap.elovetagendamento.auditoria.document.AuditoriaDocument;
import br.com.fiap.elovetagendamento.auditoria.dto.SincronizacaoResponse;
import br.com.fiap.elovetagendamento.auditoria.mapper.AuditoriaMapper;
import br.com.fiap.elovetagendamento.auditoria.model.AuditoriaOracle;
import br.com.fiap.elovetagendamento.auditoria.repository.AuditoriaMongoRepository;
import br.com.fiap.elovetagendamento.auditoria.repository.AuditoriaOracleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Copia os registros de elo_auditoria (Oracle) para a collection
 * auditoria_agendamento (MongoDB).
 * A cópia é incremental: busca no Oracle apenas os ids maiores que o último já
 * presente no MongoDB. Como o _id do documento é o próprio id_auditoria,
 * executar a sincronização mais de uma vez não duplica registros.
 */
@Service
public class AuditoriaSincronizacaoService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaSincronizacaoService.class);

    private final AuditoriaOracleRepository oracleRepository;
    private final AuditoriaMongoRepository mongoRepository;
    private final AuditoriaMapper mapper;

    public AuditoriaSincronizacaoService(AuditoriaOracleRepository oracleRepository,
                                         AuditoriaMongoRepository mongoRepository,
                                         AuditoriaMapper mapper) {
        this.oracleRepository = oracleRepository;
        this.mongoRepository = mongoRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public SincronizacaoResponse sincronizar() {
        long ultimoIdSincronizado = mongoRepository.findTopByOrderByIdDesc()
                .map(AuditoriaDocument::getId)
                .orElse(0L);

        log.info("Iniciando sincronização da auditoria: buscando em elo_auditoria os registros com id > {}",
                ultimoIdSincronizado);
        List<AuditoriaOracle> pendentes = oracleRepository.findByIdGreaterThanOrderByIdAsc(ultimoIdSincronizado);
        log.debug("{} registro(s) de auditoria pendente(s) encontrados no Oracle", pendentes.size());

        LocalDateTime agora = LocalDateTime.now();
        List<AuditoriaDocument> documentos = pendentes.stream()
                .map(auditoria -> mapper.toDocument(auditoria, agora))
                .toList();

        mongoRepository.saveAll(documentos);

        Long ultimoId = documentos.isEmpty() ? ultimoIdSincronizado : documentos.getLast().getId();
        log.info("Sincronização de auditoria concluída: {} registro(s) copiados para o MongoDB (último id_auditoria = {})",
                documentos.size(), ultimoId);

        return new SincronizacaoResponse(documentos.size(), ultimoId, agora);
    }

    public List<AuditoriaDocument> listar(Long idAgendamento) {
        return idAgendamento != null
                ? mongoRepository.findByIdRegistroAfetadoOrderByIdAsc(idAgendamento)
                : mongoRepository.findAllByOrderByIdAsc();
    }
}
