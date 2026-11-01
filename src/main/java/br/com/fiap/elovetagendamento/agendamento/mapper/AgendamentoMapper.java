package br.com.fiap.elovetagendamento.agendamento.mapper;

import br.com.fiap.elovetagendamento.agendamento.dto.AgendamentoRequest;
import br.com.fiap.elovetagendamento.agendamento.dto.AgendamentoResponse;
import br.com.fiap.elovetagendamento.agendamento.model.Agendamento;
import br.com.fiap.elovetagendamento.catalogo.dto.NomesCatalogo;
import org.springframework.stereotype.Component;

@Component
public class AgendamentoMapper {

    public AgendamentoResponse toResponse(Agendamento agendamento, NomesCatalogo nomes) {
        return new AgendamentoResponse(
                agendamento.getId(),
                agendamento.getIdPet(),
                nomes.pets().get(agendamento.getIdPet()),
                nomes.especies().get(agendamento.getIdPet()),
                agendamento.getIdResponsavel(),
                nomes.responsaveis().get(agendamento.getIdResponsavel()),
                agendamento.getIdVeterinario(),
                nomes.veterinarios().get(agendamento.getIdVeterinario()),
                agendamento.getIdClinica(),
                nomes.clinicas().get(agendamento.getIdClinica()),
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
