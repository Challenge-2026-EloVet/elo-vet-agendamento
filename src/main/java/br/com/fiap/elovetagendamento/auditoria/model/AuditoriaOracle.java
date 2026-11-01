package br.com.fiap.elovetagendamento.auditoria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Registro de elo_auditoria, gravado pela trigger trg_auditoria_agendamento
 * (via pkg_elovet.pr_audita_agendamento). Somente leitura nesta aplicação.
 * Os valores anteriores e novos já chegam em JSON, montados por pkg_elovet.fn_monta_json.
 */
@Entity
@Immutable
@Table(name = "elo_auditoria")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaOracle {

    @Id
    @Column(name = "id_auditoria")
    private Long id;

    @Column(name = "nome_usuario")
    private String nomeUsuario;

    @Column(name = "tipo_operacao")
    private String tipoOperacao;

    @Column(name = "data_hora_operacao")
    private LocalDateTime dataHoraOperacao;

    @Column(name = "tabela_afetada")
    private String tabelaAfetada;

    @Column(name = "id_registro_afetado")
    private Long idRegistroAfetado;

    @Lob
    @Column(name = "valores_anteriores")
    private String valoresAnteriores;

    @Lob
    @Column(name = "valores_novos")
    private String valoresNovos;
}
