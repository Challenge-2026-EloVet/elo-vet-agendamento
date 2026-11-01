package br.com.fiap.elovetagendamento.agendamento.exception;

public class AgendamentoNotFoundException extends RuntimeException {
    public AgendamentoNotFoundException(Long id) {
        super("Agendamento não encontrado: " + id);
    }
}
