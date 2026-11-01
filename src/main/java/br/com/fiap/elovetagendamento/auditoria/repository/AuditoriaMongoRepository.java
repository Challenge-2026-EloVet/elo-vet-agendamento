package br.com.fiap.elovetagendamento.auditoria.repository;

import br.com.fiap.elovetagendamento.auditoria.document.AuditoriaDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AuditoriaMongoRepository extends MongoRepository<AuditoriaDocument, Long> {

    Optional<AuditoriaDocument> findTopByOrderByIdDesc();

    List<AuditoriaDocument> findByIdRegistroAfetadoOrderByIdAsc(Long idRegistroAfetado);

    List<AuditoriaDocument> findAllByOrderByIdAsc();
}
