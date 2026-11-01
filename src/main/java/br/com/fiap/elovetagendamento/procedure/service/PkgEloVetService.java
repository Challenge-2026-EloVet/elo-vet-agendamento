package br.com.fiap.elovetagendamento.procedure.service;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Chama as procedures e functions do pacote pkg_elovet.
 * As procedures do pacote escrevem o resultado com DBMS_OUTPUT; por isso a
 * chamada habilita o buffer, executa a procedure e lê as linhas geradas,
 * tudo na mesma conexão.
 */
@Service
public class PkgEloVetService {

    private final JdbcTemplate jdbcTemplate;

    public PkgEloVetService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** pkg_elovet.pr_agenda_json: agendamentos (JOIN de 4 tabelas) em formato JSON. */
    public List<String> agendaJson() {
        return executarComSaida("BEGIN pkg_elovet.pr_agenda_json; END;");
    }

    /** pkg_elovet.pr_totaliza_idade_pet: soma de idades por espécie e sexo (quebra de controle). */
    public List<String> totalizaIdadePet() {
        return executarComSaida("BEGIN pkg_elovet.pr_totaliza_idade_pet; END;");
    }

    /** pkg_elovet.fn_valida_cpf: valida os dígitos verificadores do CPF. */
    public String validaCpf(String cpf) {
        return jdbcTemplate.queryForObject("SELECT pkg_elovet.fn_valida_cpf(?) FROM dual", String.class, cpf);
    }

    private List<String> executarComSaida(String blocoPlSql) {
        return jdbcTemplate.execute((ConnectionCallback<List<String>>) connection -> {
            executar(connection, "BEGIN DBMS_OUTPUT.ENABLE(NULL); END;");
            try {
                executar(connection, blocoPlSql);
                return lerSaida(connection);
            } finally {
                executar(connection, "BEGIN DBMS_OUTPUT.DISABLE; END;");
            }
        });
    }

    private void executar(Connection connection, String blocoPlSql) throws SQLException {
        try (CallableStatement statement = connection.prepareCall(blocoPlSql)) {
            statement.execute();
        }
    }

    private List<String> lerSaida(Connection connection) throws SQLException {
        List<String> linhas = new ArrayList<>();
        try (CallableStatement statement = connection.prepareCall("BEGIN DBMS_OUTPUT.GET_LINE(?, ?); END;")) {
            statement.registerOutParameter(1, Types.VARCHAR);
            statement.registerOutParameter(2, Types.INTEGER);
            while (true) {
                statement.execute();
                // status 0 = linha lida; 1 = não há mais linhas no buffer
                if (statement.getInt(2) != 0) {
                    return linhas;
                }
                String linha = statement.getString(1);
                linhas.add(linha != null ? linha : "");
            }
        }
    }
}
