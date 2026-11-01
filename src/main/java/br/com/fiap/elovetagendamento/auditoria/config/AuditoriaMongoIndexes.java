package br.com.fiap.elovetagendamento.auditoria.config;

import br.com.fiap.elovetagendamento.auditoria.service.AuditoriaSincronizacaoService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Cria os índices da collection auditoria_agendamento quando a aplicação sobe.
 * Criar um índice que já existe não tem efeito, então a operação pode rodar a cada inicialização.
 */
@Component
public class AuditoriaMongoIndexes {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaMongoIndexes.class);

    private final MongoTemplate mongoTemplate;

    public AuditoriaMongoIndexes(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void criarIndices() {
        try {
            MongoCollection<Document> collection = mongoTemplate.getCollection(AuditoriaSincronizacaoService.COLLECTION);
            collection.createIndex(Indexes.ascending("dataReferencia"));
            collection.createIndex(Indexes.ascending("id_agendamento"));
            collection.createIndex(Indexes.ascending("valores_novos.status"));
            collection.createIndex(Indexes.ascending("pet.especie"));
            collection.createIndex(Indexes.ascending("clinica.id_clinica"));
            log.info("Índices da collection {} verificados", AuditoriaSincronizacaoService.COLLECTION);
        } catch (RuntimeException exception) {
            log.warn("Não foi possível criar os índices no MongoDB: {}", exception.getMessage());
        }
    }
}
