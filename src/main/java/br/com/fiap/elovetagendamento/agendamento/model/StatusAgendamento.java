package br.com.fiap.elovetagendamento.agendamento.model;

public enum StatusAgendamento {
    AGENDADO,
    CONFIRMADO,
    EM_ATENDIMENTO,
    CONCLUIDO,
    CANCELADO,
    NAO_COMPARECEU;

    public boolean isFinal() {
        return this == CONCLUIDO || this == CANCELADO || this == NAO_COMPARECEU;
    }
}
