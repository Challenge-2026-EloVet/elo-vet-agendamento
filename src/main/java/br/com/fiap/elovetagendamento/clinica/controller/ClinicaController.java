package br.com.fiap.elovetagendamento.clinica.controller;

import br.com.fiap.elovetagendamento.clinica.dto.ClinicaResponse;
import br.com.fiap.elovetagendamento.clinica.service.ClinicaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Clínicas para o app mobile do tutor (busca por especialidade e por proximidade). */
@RestController
@RequestMapping("/clinicas")
public class ClinicaController {

    private final ClinicaService clinicaService;

    public ClinicaController(ClinicaService clinicaService) {
        this.clinicaService = clinicaService;
    }

    @GetMapping
    public List<ClinicaResponse> listar(@RequestParam(required = false) Double latitude,
                                        @RequestParam(required = false) Double longitude,
                                        @RequestParam(required = false) String especialidade) {
        return clinicaService.listar(latitude, longitude, especialidade);
    }

    @GetMapping("/{id}")
    public ClinicaResponse buscar(@PathVariable Long id,
                                  @RequestParam(required = false) Double latitude,
                                  @RequestParam(required = false) Double longitude) {
        return clinicaService.buscar(id, latitude, longitude);
    }
}
