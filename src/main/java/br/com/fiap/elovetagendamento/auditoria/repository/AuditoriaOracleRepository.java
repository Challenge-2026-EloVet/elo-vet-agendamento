package br.com.fiap.elovetagendamento.auditoria.repository;

import br.com.fiap.elovetagendamento.auditoria.model.AuditoriaOracle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaOracleRepository extends JpaRepository<AuditoriaOracle, Long> {

    List<AuditoriaOracle> findByIdGreaterThanOrderByIdAsc(Long id);
}
