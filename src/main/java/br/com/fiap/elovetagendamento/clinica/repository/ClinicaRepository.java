package br.com.fiap.elovetagendamento.clinica.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Consultas das clínicas para o app do tutor.
 * - endereço e coordenadas: elo_endereco
 * - especialidades: veterinários da clínica (elo_clinica_veterinario x elo_veterinario_especialidade)
 * - acessibilidade: elo_clinica_acessibilidade x elo_acessibilidade
 */
@Repository
public class ClinicaRepository {

    private static final String SELECT_CLINICA = """
            SELECT c.id_clinica, c.nome, c.telefone, c.avaliacao,
                   e.logradouro, e.numero, e.complemento, e.bairro, e.cidade, e.estado,
                   e.latitude, e.longitude
            FROM   elo_clinica c
            JOIN   elo_endereco e ON e.id_endereco = c.id_endereco
            """;

    private final JdbcTemplate jdbcTemplate;

    public ClinicaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ClinicaRow> listar() {
        return jdbcTemplate.query(SELECT_CLINICA + " ORDER BY c.nome", this::mapear);
    }

    public Optional<ClinicaRow> buscar(Long idClinica) {
        return jdbcTemplate.query(SELECT_CLINICA + " WHERE c.id_clinica = ?", this::mapear, idClinica)
                .stream()
                .findFirst();
    }

    /** Especialidades de cada clínica, obtidas pelos veterinários que atendem nela. */
    public Map<Long, List<String>> especialidadesPorClinica() {
        return agrupar("""
                SELECT DISTINCT cv.id_clinica, es.nome
                FROM   elo_clinica_veterinario cv
                JOIN   elo_veterinario_especialidade ve ON ve.id_veterinario = cv.id_veterinario
                JOIN   elo_especialidade es ON es.id_especialidade = ve.id_especialidade
                ORDER  BY es.nome
                """);
    }

    public Map<Long, List<String>> acessibilidadePorClinica() {
        return agrupar("""
                SELECT ca.id_clinica, a.descricao
                FROM   elo_clinica_acessibilidade ca
                JOIN   elo_acessibilidade a ON a.id_acessibilidade = ca.id_acessibilidade
                ORDER  BY a.descricao
                """);
    }

    private Map<Long, List<String>> agrupar(String sql) {
        Map<Long, List<String>> resultado = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            resultado.computeIfAbsent(rs.getLong(1), id -> new java.util.ArrayList<>()).add(rs.getString(2));
        });
        return resultado;
    }

    private ClinicaRow mapear(ResultSet rs, int linha) throws SQLException {
        return new ClinicaRow(
                rs.getLong("id_clinica"),
                rs.getString("nome"),
                rs.getString("telefone"),
                numeroOuNulo(rs, "avaliacao"),
                rs.getString("logradouro"),
                rs.getString("numero"),
                rs.getString("complemento"),
                rs.getString("bairro"),
                rs.getString("cidade"),
                rs.getString("estado"),
                numeroOuNulo(rs, "latitude"),
                numeroOuNulo(rs, "longitude")
        );
    }

    private static Double numeroOuNulo(ResultSet rs, String coluna) throws SQLException {
        double valor = rs.getDouble(coluna);
        return rs.wasNull() ? null : valor;
    }

}
