package br.com.fiap.elovetagendamento.catalogo.controller;

import br.com.fiap.elovetagendamento.catalogo.dto.Opcoes;
import br.com.fiap.elovetagendamento.catalogo.repository.CatalogoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dados cadastrais usados pelo portal para montar o formulário de agendamento. */
@RestController
@RequestMapping("/catalogo")
public class CatalogoController {

    private final CatalogoRepository catalogoRepository;

    public CatalogoController(CatalogoRepository catalogoRepository) {
        this.catalogoRepository = catalogoRepository;
    }

    @GetMapping("/clinicas")
    public List<Opcoes.Clinica> clinicas() {
        return catalogoRepository.clinicas();
    }

    @GetMapping("/clinicas/{idClinica}/veterinarios")
    public List<Opcoes.Veterinario> veterinarios(@PathVariable Long idClinica) {
        return catalogoRepository.veterinariosDaClinica(idClinica);
    }

    @GetMapping("/responsaveis")
    public List<Opcoes.Responsavel> responsaveis() {
        return catalogoRepository.responsaveis();
    }

    @GetMapping("/responsaveis/{idResponsavel}/pets")
    public List<Opcoes.Pet> pets(@PathVariable Long idResponsavel) {
        return catalogoRepository.petsDoResponsavel(idResponsavel);
    }
}
