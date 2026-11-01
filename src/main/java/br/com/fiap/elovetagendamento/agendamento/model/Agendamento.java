package br.com.fiap.elovetagendamento.agendamento.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "elo_agendamento")
@Getter
@Setter
@NoArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "elo_agendamento_seq")
    @SequenceGenerator(name = "elo_agendamento_seq", sequenceName = "ELO_AGENDAMENTO_ID_AGENDAMENTO_SEQ", allocationSize = 1)
    @Column(name = "id_agendamento")
    private Long id;

    @Column(name = "id_pet", nullable = false)
    private Long idPet;

    @Column(name = "id_responsavel", nullable = false)
    private Long idResponsavel;

    @Column(name = "id_veterinario", nullable = false)
    private Long idVeterinario;

    @Column(name = "id_clinica", nullable = false)
    private Long idClinica;

    @Column(name = "data_hora_agendamento", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusAgendamento status;

    @Column(name = "observacao", length = 500)
    private String observacao;
}
