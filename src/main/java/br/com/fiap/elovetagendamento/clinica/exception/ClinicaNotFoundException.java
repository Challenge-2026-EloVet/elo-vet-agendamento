package br.com.fiap.elovetagendamento.clinica.exception;

public class ClinicaNotFoundException extends RuntimeException {
    public ClinicaNotFoundException(Long id) {
        super("Clínica não encontrada: " + id);
    }
}
