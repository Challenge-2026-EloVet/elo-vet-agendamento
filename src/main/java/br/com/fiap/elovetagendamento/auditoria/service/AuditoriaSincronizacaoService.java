package br.com.fiap.elovetagendamento.auditoria.service;

import br.com.fiap.elovetagendamento.auditoria.dto.SincronizacaoResponse;
import br.com.fiap.elovetagendamento.procedure.service.PkgEloVetService;
import org.bson.BsonInvalidOperationException;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Exporta a auditoria do Oracle para o MongoDB.
 * O JSON é gerado pelo próprio banco relacional, na function pkg_elovet.fn_exporta_valores_novos,
 * que devolve um array com as auditorias do dia anterior: o snapshot do agendamento e os dados
 * do pet, responsável, veterinário e clínica. Cada objeto do array é gravado como veio, como um
 * documento da collection auditoria_agendamento, acrescido de:
 * - _id: o id_auditoria do Oracle;
 * - dataReferencia: o dia exportado (AAAA-MM-DD);
 * - exportadoEm: o momento da gravação.
 * Executar a exportação mais de uma vez para o mesmo dia substitui os documentos daquele dia.
 */
@Service
public class AuditoriaSincronizacaoService {

    public static final String COLLECTION = "auditoria_agendamento";

    private static final Logger log = LoggerFactory.getLogger(AuditoriaSincronizacaoService.class);

    private final PkgEloVetService pkgEloVetService;
    private final MongoTemplate mongoTemplate;

    public AuditoriaSincronizacaoService(PkgEloVetService pkgEloVetService, MongoTemplate mongoTemplate) {
        this.pkgEloVetService = pkgEloVetService;
        this.mongoTemplate = mongoTemplate;
    }

    public SincronizacaoResponse sincronizar() {
        // A data vem do próprio Oracle para coincidir com o TRUNC(SYSDATE) - 1 usado pela function
        LocalDate dataReferencia = pkgEloVetService.dataReferenciaExportacao();
        log.info("Iniciando exportação da auditoria de {} (pkg_elovet.fn_exporta_valores_novos -> MongoDB)",
                dataReferencia);

        String json = pkgEloVetService.exportaValoresNovos();
        LocalDateTime agora = LocalDateTime.now();
        List<Document> documentos = converter(json, dataReferencia, agora);

        long substituidos = mongoTemplate
                .remove(Query.query(Criteria.where("dataReferencia").is(dataReferencia.toString())), COLLECTION)
                .getDeletedCount();
        if (!documentos.isEmpty()) {
            mongoTemplate.insert(documentos, COLLECTION);
        }

        log.info("Exportação de {} concluída: {} documento(s) gravados no MongoDB ({} substituídos de execução anterior)",
                dataReferencia, documentos.size(), substituidos);
        return new SincronizacaoResponse(dataReferencia, documentos.size(), substituidos, agora);
    }

    public List<Document> listar(Long idAgendamento, LocalDate dataReferencia) {
        Query query = new Query();
        if (idAgendamento != null) {
            query.addCriteria(Criteria.where("id_agendamento").is(idAgendamento));
        }
        if (dataReferencia != null) {
            query.addCriteria(Criteria.where("dataReferencia").is(dataReferencia.toString()));
        }
        query.with(Sort.by(Sort.Order.desc("dataReferencia"), Sort.Order.asc("_id")));
        return mongoTemplate.find(query, Document.class, COLLECTION);
    }

    private List<Document> converter(String json, LocalDate dataReferencia, LocalDateTime agora) {
        if (json == null || json.isBlank()) {
            // A function devolve NULL quando ocorre erro inesperado no Oracle
            throw new ExportacaoInvalidaException(
                    "pkg_elovet.fn_exporta_valores_novos não retornou dados (verifique a saída do DBMS_OUTPUT no Oracle)");
        }
        List<Document> itens;
        try {
            // O parser do BSON só aceita objetos na raiz, por isso o array é envolvido em um objeto
            itens = Document.parse("{\"itens\": " + json + "}").getList("itens", Document.class);
        } catch (JsonParseException | BsonInvalidOperationException | ClassCastException exception) {
            throw new ExportacaoInvalidaException(
                    "O retorno de pkg_elovet.fn_exporta_valores_novos não é um array JSON válido", exception);
        }

        Date exportadoEm = Date.from(agora.atZone(ZoneId.systemDefault()).toInstant());
        return itens.stream()
                .map(item -> {
                    Object idAuditoria = item.get("id_auditoria");
                    if (idAuditoria == null) {
                        throw new ExportacaoInvalidaException(
                                "Objeto exportado sem id_auditoria: " + item.toJson());
                    }
                    Document documento = new Document("_id", idAuditoria);
                    documento.putAll(item);
                    documento.append("dataReferencia", dataReferencia.toString())
                            .append("exportadoEm", exportadoEm);
                    return documento;
                })
                .toList();
    }
}
