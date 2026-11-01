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
import br.com.fiap.elovetagendamento.catalogo.dto.NomesCatalogo;
import br.com.fiap.elovetagendamento.catalogo.repository.CatalogoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de agendamento de consultas. Toda gravação em elo_agendamento dispara a
 * trigger trg_auditoria_agendamento, que chama pkg_elovet.pr_audita_agendamento
 * e registra a operação em elo_auditoria.
 */
@Service
public class AgendamentoService {

    private static final Logger log = LoggerFactory.getLogger(AgendamentoService.class);

    private final AgendamentoRepository agendamentoRepository;
    private final VinculoRepository vinculoRepository;
    private final CatalogoRepository catalogoRepository;
    private final AgendamentoMapper mapper;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              VinculoRepository vinculoRepository,
                              CatalogoRepository catalogoRepository,
                              AgendamentoMapper mapper) {
        this.agendamentoRepository = agendamentoRepository;
        this.vinculoRepository = vinculoRepository;
        this.catalogoRepository = catalogoRepository;
        this.mapper = mapper;
    }

    @Transactional
    public AgendamentoResponse criar(AgendamentoRequest request) {
        log.info("Criando agendamento: pet={}, responsavel={}, veterinario={}, clinica={}, dataHora={}",
                request.idPet(), request.idResponsavel(), request.idVeterinario(), request.idClinica(), request.dataHora());
        validarVinculos(request);

        Agendamento agendamento = new Agendamento();
        mapper.copyToEntity(request, agendamento);
        agendamento.setStatus(request.status() != null ? request.status() : StatusAgendamento.AGENDADO);

        Agendamento salvo = agendamentoRepository.save(agendamento);
        log.info("Agendamento {} criado com status {} (auditoria INSERT registrada pela trigger no Oracle)",
                salvo.getId(), salvo.getStatus());
        return toResponse(salvo);
    }

    @Transactional
    public AgendamentoResponse atualizar(Long id, AgendamentoRequest request) {
        log.info("Atualizando agendamento {}: pet={}, responsavel={}, veterinario={}, clinica={}, dataHora={}",
                id, request.idPet(), request.idResponsavel(), request.idVeterinario(), request.idClinica(), request.dataHora());
        Agendamento agendamento = buscarEntidade(id);
        validarAlteravel(agendamento);
        validarVinculos(request);

        LocalDateTime dataHoraAnterior = agendamento.getDataHora();
        mapper.copyToEntity(request, agendamento);
        if (request.status() != null) {
            agendamento.setStatus(request.status());
        }

        Agendamento salvo = agendamentoRepository.save(agendamento);
        log.info("Agendamento {} atualizado: dataHora {} -> {}, status {} (auditoria UPDATE registrada pela trigger no Oracle)",
                id, dataHoraAnterior, salvo.getDataHora(), salvo.getStatus());
        return toResponse(salvo);
    }

    @Transactional
    public AgendamentoResponse alterarStatus(Long id, StatusRequest request) {
        Agendamento agendamento = buscarEntidade(id);
        validarAlteravel(agendamento);

        StatusAgendamento statusAnterior = agendamento.getStatus();
        agendamento.setStatus(request.status());
        if (request.observacao() != null) {
            agendamento.setObservacao(request.observacao());
        }

        Agendamento salvo = agendamentoRepository.save(agendamento);
        log.info("Agendamento {} mudou de status: {} -> {} (auditoria UPDATE registrada pela trigger no Oracle)",
                id, statusAnterior, salvo.getStatus());
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(Long id) {
        return toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(Long idResponsavel, Long idPet, Long idVeterinario, StatusAgendamento status) {
        NomesCatalogo nomes = catalogoRepository.nomes();
        List<AgendamentoResponse> resultado = agendamentoRepository.buscar(idResponsavel, idPet, idVeterinario, status)
                .stream()
                .map(agendamento -> mapper.toResponse(agendamento, nomes))
                .toList();
        log.debug("Consulta de agendamentos (responsavel={}, pet={}, veterinario={}, status={}): {} resultado(s)",
                idResponsavel, idPet, idVeterinario, status, resultado.size());
        return resultado;
    }

    private AgendamentoResponse toResponse(Agendamento agendamento) {
        return mapper.toResponse(agendamento, catalogoRepository.nomes());
    }

    private Agendamento buscarEntidade(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Agendamento {} não encontrado", id);
                    return new AgendamentoNotFoundException(id);
                });
    }

    private void validarAlteravel(Agendamento agendamento) {
        if (agendamento.getStatus().isFinal()) {
            log.warn("Alteração recusada: agendamento {} já está com status final {}", agendamento.getId(), agendamento.getStatus());
            throw new AgendamentoRegraException(
                    "Agendamento com status " + agendamento.getStatus() + " não pode mais ser alterado");
        }
    }

    private void validarVinculos(AgendamentoRequest request) {
        if (!vinculoRepository.petPertenceAoResponsavel(request.idPet(), request.idResponsavel())) {
            log.warn("Vínculo inválido: pet {} não pertence ao responsável {}", request.idPet(), request.idResponsavel());
            throw new AgendamentoRegraException("O pet informado não está vinculado a este responsável");
        }
        if (!vinculoRepository.veterinarioAtendeNaClinica(request.idVeterinario(), request.idClinica())) {
            log.warn("Vínculo inválido: veterinário {} não atende na clínica {}", request.idVeterinario(), request.idClinica());
            throw new AgendamentoRegraException("O veterinário informado não atende nesta clínica");
        }
    }
}
