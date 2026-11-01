package br.com.fiap.elovetagendamento.procedure.controller;

import br.com.fiap.elovetagendamento.procedure.service.PkgEloVetService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Expõe as procedures e functions do pkg_elovet, chamadas a partir da aplicação.
 */
@RestController
@RequestMapping("/procedures")
public class ProcedureController {

    private final PkgEloVetService pkgEloVetService;

    public ProcedureController(PkgEloVetService pkgEloVetService) {
        this.pkgEloVetService = pkgEloVetService;
    }

    /**
     * Executa pkg_elovet.pr_agenda_json. A procedure monta o JSON manualmente
     * (fn_monta_json); quando há agendamentos, a saída é devolvida como JSON.
     */
    @GetMapping("/agenda-json")
    public ResponseEntity<String> agendaJson() {
        String saida = String.join("\n", pkgEloVetService.agendaJson());
        MediaType tipo = saida.stripLeading().startsWith("[") ? MediaType.APPLICATION_JSON : MediaType.TEXT_PLAIN;
        return ResponseEntity.ok().contentType(tipo).body(saida);
    }

    /** Executa pkg_elovet.pr_totaliza_idade_pet e devolve o relatório gerado pela procedure. */
    @GetMapping(value = "/totaliza-idade-pet", produces = MediaType.TEXT_PLAIN_VALUE)
    public String totalizaIdadePet() {
        List<String> linhas = pkgEloVetService.totalizaIdadePet();
        return String.join("\n", linhas);
    }

    /** Executa pkg_elovet.fn_valida_cpf. */
    @GetMapping("/valida-cpf/{cpf}")
    public Map<String, String> validaCpf(@PathVariable String cpf) {
        return Map.of("cpf", cpf, "resultado", pkgEloVetService.validaCpf(cpf));
    }
}
