package br.com.fiap.elovetagendamento.catalogo.repository;

import br.com.fiap.elovetagendamento.catalogo.dto.NomesCatalogo;
import br.com.fiap.elovetagendamento.catalogo.dto.Opcoes;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de apoio às tabelas cadastrais do modelo relacional
 * (clínicas, veterinários, responsáveis e pets).
 */
@Repository
public class CatalogoRepository {

    private final JdbcTemplate jdbcTemplate;

    public CatalogoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Opcoes.Clinica> clinicas() {
        return jdbcTemplate.query(
                "SELECT id_clinica, nome FROM elo_clinica ORDER BY nome",
                (rs, i) -> new Opcoes.Clinica(rs.getLong("id_clinica"), rs.getString("nome")));
    }

    public List<Opcoes.Veterinario> veterinariosDaClinica(Long idClinica) {
        return jdbcTemplate.query("""
                        SELECT v.id_veterinario, v.nome_completo, v.crmv
                        FROM   elo_veterinario v
                        JOIN   elo_clinica_veterinario cv ON cv.id_veterinario = v.id_veterinario
                        WHERE  cv.id_clinica = ?
                        ORDER  BY v.nome_completo
                        """,
                (rs, i) -> new Opcoes.Veterinario(rs.getLong("id_veterinario"),
                        rs.getString("nome_completo"), rs.getString("crmv")),
                idClinica);
    }

    public List<Opcoes.Responsavel> responsaveis() {
        return jdbcTemplate.query(
                "SELECT id_responsavel, nome_completo FROM elo_responsavel ORDER BY nome_completo",
                (rs, i) -> new Opcoes.Responsavel(rs.getLong("id_responsavel"), rs.getString("nome_completo")));
    }

    public List<Opcoes.Pet> petsDoResponsavel(Long idResponsavel) {
        return jdbcTemplate.query("""
                        SELECT p.id_pet, p.nome, p.especie, p.raca
                        FROM   elo_pet p
                        JOIN   elo_pet_responsavel pr ON pr.id_pet = p.id_pet
                        WHERE  pr.id_responsavel = ?
                        ORDER  BY p.nome
                        """,
                (rs, i) -> new Opcoes.Pet(rs.getLong("id_pet"), rs.getString("nome"),
                        rs.getString("especie"), rs.getString("raca")),
                idResponsavel);
    }

    public NomesCatalogo nomes() {
        return new NomesCatalogo(
                mapa("SELECT id_pet, nome FROM elo_pet"),
                mapa("SELECT id_pet, especie FROM elo_pet"),
                mapa("SELECT id_responsavel, nome_completo FROM elo_responsavel"),
                mapa("SELECT id_veterinario, nome_completo FROM elo_veterinario"),
                mapa("SELECT id_clinica, nome FROM elo_clinica")
        );
    }

    private Map<Long, String> mapa(String sql) {
        Map<Long, String> resultado = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            resultado.put(rs.getLong(1), rs.getString(2));
        });
        return resultado;
    }
}
