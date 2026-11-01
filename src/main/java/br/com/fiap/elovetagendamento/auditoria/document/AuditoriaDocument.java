package br.com.fiap.elovetagendamento.auditoria.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Documento da collection auditoria_agendamento no MongoDB.
 * O _id reaproveita o id_auditoria do Oracle: reprocessar um registro
 * sobrescreve o mesmo documento, sem duplicar.
 * Os valores anteriores e novos são guardados como subdocumentos (e não como texto),
 * o que permite consultá-los diretamente, por exemplo: { "valoresNovos.status": "CANCELADO" }.
 */
@Document(collection = "auditoria_agendamento")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaDocument {

    @Id
    private Long id;

    private String usuario;

    @Indexed
    private String operacao;

    private LocalDateTime dataHoraOperacao;

    private String tabelaAfetada;

    @Indexed
    private Long idRegistroAfetado;

    private Map<String, Object> valoresAnteriores;

    private Map<String, Object> valoresNovos;

    private LocalDateTime sincronizadoEm;
}
