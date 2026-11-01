package br.com.fiap.elovetagendamento.agendamento.service;

import br.com.fiap.elovetagendamento.agendamento.dto.AgendamentoRequest;
import br.com.fiap.elovetagendamento.agendamento.dto.AgendamentoResponse;
import br.com.fiap.elovetagendamento.agendamento.dto.StatusRequest;
import br.com.fiap.elovetagendamento.agendamento.exception.AgendamentoNotFoundException;
import br.com.fiap.elovetagendamento.agendamento.exception.AgendamentoRegraException;
import br.com.fiap.elovetagendamento.agendamento.mapper.AgendamentoMapper;
import br.com.fiap.elovetagendamento.agendamento.model.Agendamento;
import br.com.fiap.elovetagendamento.agendamento.model.StatusAgendamento;
import br.com.fiap.elovetagendamento.agendamento.repository.AgendamentoRepository;
import br.com.fiap.elovetagendamento.agendamento.repository.VinculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de agendamento de consultas. Toda gravação em elo_agendamento dispara a
 * trigger trg_auditoria_agendamento, que chama pkg_elovet.pr_audita_agendamento
 * e registra a operação em elo_auditoria.
 */
@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final VinculoRepository vinculoRepository;
    private final AgendamentoMapper mapper;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              VinculoRepository vinculoRepository,
                              AgendamentoMapper mapper) {
        this.agendamentoRepository = agendamentoRepository;
        this.vinculoRepository = vinculoRepository;
        this.mapper = mapper;
    }

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        validarVinculos(request);

        Agendamento agendamento = new Agendamento();
        mapper.copyToEntity(request, agendamento);
        agendamento.setStatus(request.status() != null ? request.status() : StatusAgendamento.AGENDADO);

        return mapper.toResponse(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoResponse atualizar(Long id, AgendamentoRequest request) {
        Agendamento agendamento = buscarEntidade(id);
        validarAlteravel(agendamento);
        validarVinculos(request);

        mapper.copyToEntity(request, agendamento);
        if (request.status() != null) {
            agendamento.setStatus(request.status());
        }

        return mapper.toResponse(agendamentoRepository.save(agendamento));
    }

    @Transactional
    public AgendamentoResponse alterarStatus(Long id, StatusRequest request) {
        Agendamento agendamento = buscarEntidade(id);
        validarAlteravel(agendamento);

        agendamento.setStatus(request.status());
        if (request.observacao() != null) {
            agendamento.setObservacao(request.observacao());
        }

        return mapper.toResponse(agendamentoRepository.save(agendamento));
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(Long id) {
        return mapper.toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(Long idResponsavel, Long idPet, Long idVeterinario, StatusAgendamento status) {
        return agendamentoRepository.buscar(idResponsavel, idPet, idVeterinario, status).stream()
                .map(mapper::toResponse)
                .toList();
    }

    private Agendamento buscarEntidade(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new AgendamentoNotFoundException(id));
    }

    private void validarAlteravel(Agendamento agendamento) {
        if (agendamento.getStatus().isFinal()) {
            throw new AgendamentoRegraException(
                    "Agendamento com status " + agendamento.getStatus() + " não pode mais ser alterado");
        }
    }

    private void validarVinculos(AgendamentoRequest request) {
        if (!vinculoRepository.petPertenceAoResponsavel(request.idPet(), request.idResponsavel())) {
            throw new AgendamentoRegraException("O pet informado não está vinculado a este responsável");
        }
        if (!vinculoRepository.veterinarioAtendeNaClinica(request.idVeterinario(), request.idClinica())) {
            throw new AgendamentoRegraException("O veterinário informado não atende nesta clínica");
        }
    }
}
