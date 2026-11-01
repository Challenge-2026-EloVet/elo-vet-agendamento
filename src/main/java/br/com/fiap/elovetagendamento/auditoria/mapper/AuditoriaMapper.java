package br.com.fiap.elovetagendamento.auditoria.mapper;

import br.com.fiap.elovetagendamento.auditoria.document.AuditoriaDocument;
import br.com.fiap.elovetagendamento.auditoria.model.AuditoriaOracle;
import org.bson.Document;
import org.bson.json.JsonParseException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class AuditoriaMapper {

    public AuditoriaDocument toDocument(AuditoriaOracle auditoria, LocalDateTime sincronizadoEm) {
        return new AuditoriaDocument(
                auditoria.getId(),
                auditoria.getNomeUsuario(),
                auditoria.getTipoOperacao(),
                auditoria.getDataHoraOperacao(),
                auditoria.getTabelaAfetada(),
                auditoria.getIdRegistroAfetado(),
                toMap(auditoria.getValoresAnteriores()),
                toMap(auditoria.getValoresNovos()),
                sincronizadoEm
        );
    }

    /**
     * Converte o JSON gerado pelo Oracle em subdocumento. Se o texto não for um JSON
     * válido, ele é preservado no campo "conteudoOriginal" em vez de ser descartado.
     */
    private Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return Document.parse(json);
        } catch (JsonParseException exception) {
            return Map.of("conteudoOriginal", json);
        }
    }
}
