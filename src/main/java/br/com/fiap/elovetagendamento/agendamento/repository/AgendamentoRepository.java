package br.com.fiap.elovetagendamento.agendamento.repository;

import br.com.fiap.elovetagendamento.agendamento.model.Agendamento;
import br.com.fiap.elovetagendamento.agendamento.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("""
            select a from Agendamento a
            where (:idResponsavel is null or a.idResponsavel = :idResponsavel)
              and (:idPet is null or a.idPet = :idPet)
              and (:idVeterinario is null or a.idVeterinario = :idVeterinario)
              and (:status is null or a.status = :status)
            order by a.dataHora
            """)
    List<Agendamento> buscar(@Param("idResponsavel") Long idResponsavel,
                             @Param("idPet") Long idPet,
                             @Param("idVeterinario") Long idVeterinario,
                             @Param("status") StatusAgendamento status);
}
