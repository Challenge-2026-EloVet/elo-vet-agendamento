package br.com.fiap.elovetagendamento.agendamento.controller;

import br.com.fiap.elovetagendamento.agendamento.dto.AgendamentoRequest;
import br.com.fiap.elovetagendamento.agendamento.dto.AgendamentoResponse;
import br.com.fiap.elovetagendamento.agendamento.dto.StatusRequest;
import br.com.fiap.elovetagendamento.agendamento.model.StatusAgendamento;
import br.com.fiap.elovetagendamento.agendamento.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoResponse criar(@RequestBody @Valid AgendamentoRequest request) {
        return agendamentoService.criar(request);
    }

    @PutMapping("/{id}")
    public AgendamentoResponse atualizar(@PathVariable Long id, @RequestBody @Valid AgendamentoRequest request) {
        return agendamentoService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    public AgendamentoResponse alterarStatus(@PathVariable Long id, @RequestBody @Valid StatusRequest request) {
        return agendamentoService.alterarStatus(id, request);
    }

    @GetMapping("/{id}")
    public AgendamentoResponse buscar(@PathVariable Long id) {
        return agendamentoService.buscar(id);
    }

    @GetMapping
    public List<AgendamentoResponse> listar(@RequestParam(required = false) Long idResponsavel,
                                            @RequestParam(required = false) Long idPet,
                                            @RequestParam(required = false) Long idVeterinario,
                                            @RequestParam(required = false) StatusAgendamento status) {
        return agendamentoService.listar(idResponsavel, idPet, idVeterinario, status);
    }
}
