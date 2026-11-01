package br.com.fiap.elovetagendamento.agendamento.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Consultas às tabelas de vínculo do modelo relacional, usadas para validar
 * a consistência de um agendamento antes de gravá-lo.
 */
@Repository
public class VinculoRepository {

    private final JdbcTemplate jdbcTemplate;

    public VinculoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean petPertenceAoResponsavel(Long idPet, Long idResponsavel) {
        return existe("SELECT COUNT(*) FROM elo_pet_responsavel WHERE id_pet = ? AND id_responsavel = ?",
                idPet, idResponsavel);
    }

    public boolean veterinarioAtendeNaClinica(Long idVeterinario, Long idClinica) {
        return existe("SELECT COUNT(*) FROM elo_clinica_veterinario WHERE id_veterinario = ? AND id_clinica = ?",
                idVeterinario, idClinica);
    }

    private boolean existe(String sql, Object... parametros) {
        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, parametros);
        return total != null && total > 0;
    }
}
