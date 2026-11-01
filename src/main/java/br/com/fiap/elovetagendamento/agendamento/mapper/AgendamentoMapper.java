package br.com.fiap.elovetagendamento.agendamento.mapper;

import br.com.fiap.elovetagendamento.agendamento.dto.AgendamentoRequest;
import br.com.fiap.elovetagendamento.agendamento.dto.AgendamentoResponse;
import br.com.fiap.elovetagendamento.agendamento.model.Agendamento;
import org.springframework.stereotype.Component;

@Component
public class AgendamentoMapper {

    public AgendamentoResponse toResponse(Agendamento agendamento) {
        return new AgendamentoResponse(
                agendamento.getId(),
                agendamento.getIdPet(),
                agendamento.getIdResponsavel(),
                agendamento.getIdVeterinario(),
                agendamento.getIdClinica(),
                agendamento.getDataHora(),
                agendamento.getStatus(),
                agendamento.getObservacao()
        );
    }

    public void copyToEntity(AgendamentoRequest request, Agendamento agendamento) {
        agendamento.setIdPet(request.idPet());
        agendamento.setIdResponsavel(request.idResponsavel());
        agendamento.setIdVeterinario(request.idVeterinario());
        agendamento.setIdClinica(request.idClinica());
        agendamento.setDataHora(request.dataHora());
        agendamento.setObservacao(request.observacao());
    }
}
