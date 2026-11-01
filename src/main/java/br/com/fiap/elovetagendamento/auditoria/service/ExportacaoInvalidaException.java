package br.com.fiap.elovetagendamento.auditoria.service;

public class ExportacaoInvalidaException extends RuntimeException {
    public ExportacaoInvalidaException(String message) {
        super(message);
    }

    public ExportacaoInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}
