package br.com.fiap.elovetagendamento.procedure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
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

    private static final Logger log = LoggerFactory.getLogger(PkgEloVetService.class);

    private final JdbcTemplate jdbcTemplate;

    public PkgEloVetService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** pkg_elovet.pr_agenda_json: agendamentos (JOIN de 4 tabelas) em formato JSON. */
    public List<String> agendaJson() {
        return executarComSaida("pkg_elovet.pr_agenda_json");
    }

    /** pkg_elovet.pr_totaliza_idade_pet: soma de idades por espécie e sexo (quebra de controle). */
    public List<String> totalizaIdadePet() {
        return executarComSaida("pkg_elovet.pr_totaliza_idade_pet");
    }

    /**
     * pkg_elovet.fn_exporta_valores_novos: array JSON com os valores novos auditados
     * no dia anterior, montado pelo próprio Oracle.
     */
    public String exportaValoresNovos() {
        log.info("Executando pkg_elovet.fn_exporta_valores_novos no Oracle");
        long inicio = System.currentTimeMillis();
        String json = jdbcTemplate.queryForObject("SELECT pkg_elovet.fn_exporta_valores_novos FROM dual", String.class);
        log.info("pkg_elovet.fn_exporta_valores_novos concluída em {} ms ({} caracteres de JSON)",
                System.currentTimeMillis() - inicio, json == null ? 0 : json.length());
        return json;
    }

    /** Dia anterior segundo o relógio do Oracle: o mesmo TRUNC(SYSDATE) - 1 usado pela exportação. */
    public LocalDate dataReferenciaExportacao() {
        return jdbcTemplate.queryForObject("SELECT TRUNC(SYSDATE) - 1 FROM dual", LocalDate.class);
    }

    /** pkg_elovet.fn_valida_cpf: valida os dígitos verificadores do CPF. */
    public String validaCpf(String cpf) {
        log.info("Executando pkg_elovet.fn_valida_cpf para o CPF {}", mascararCpf(cpf));
        String resultado = jdbcTemplate.queryForObject("SELECT pkg_elovet.fn_valida_cpf(?) FROM dual", String.class, cpf);
        log.info("pkg_elovet.fn_valida_cpf retornou {} para o CPF {}", resultado, mascararCpf(cpf));
        return resultado;
    }

    private List<String> executarComSaida(String procedure) {
        log.info("Executando {} no Oracle", procedure);
        long inicio = System.currentTimeMillis();

        List<String> saida = jdbcTemplate.execute((ConnectionCallback<List<String>>) connection -> {
            executar(connection, "BEGIN DBMS_OUTPUT.ENABLE(NULL); END;");
            try {
                executar(connection, "BEGIN " + procedure + "; END;");
                return lerSaida(connection);
            } finally {
                executar(connection, "BEGIN DBMS_OUTPUT.DISABLE; END;");
            }
        });

        log.info("{} concluída em {} ms: {} linha(s) lidas do DBMS_OUTPUT",
                procedure, System.currentTimeMillis() - inicio, saida == null ? 0 : saida.size());
        return saida;
    }

    /** Evita gravar o CPF completo em log: mantém apenas os dois últimos dígitos. */
    private String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() < 2) {
            return "***";
        }
        return "*********" + cpf.substring(cpf.length() - 2);
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
