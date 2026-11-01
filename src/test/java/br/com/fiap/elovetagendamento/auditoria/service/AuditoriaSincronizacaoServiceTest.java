package br.com.fiap.elovetagendamento.auditoria.service;

import br.com.fiap.elovetagendamento.auditoria.document.AuditoriaDocument;
import br.com.fiap.elovetagendamento.auditoria.dto.SincronizacaoResponse;
import br.com.fiap.elovetagendamento.auditoria.mapper.AuditoriaMapper;
import br.com.fiap.elovetagendamento.auditoria.model.AuditoriaOracle;
import br.com.fiap.elovetagendamento.auditoria.repository.AuditoriaMongoRepository;
import br.com.fiap.elovetagendamento.auditoria.repository.AuditoriaOracleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaSincronizacaoServiceTest {

    @Mock
    private AuditoriaOracleRepository oracleRepository;

    @Mock
    private AuditoriaMongoRepository mongoRepository;

    private AuditoriaSincronizacaoService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaSincronizacaoService(oracleRepository, mongoRepository, new AuditoriaMapper());
    }

    @Test
    void deveBuscarApenasRegistrosPosterioresAoUltimoSincronizado() {
        when(mongoRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(documento(10L)));
        when(oracleRepository.findByIdGreaterThanOrderByIdAsc(10L)).thenReturn(List.of(
                auditoria(11L, "INSERT", null, "{\"id_agendamento\":9,\"status\":\"AGENDADO\"}"),
                auditoria(12L, "UPDATE", "{\"id_agendamento\":9,\"status\":\"AGENDADO\"}",
                        "{\"id_agendamento\":9,\"status\":\"CANCELADO\"}")
        ));

        SincronizacaoResponse resposta = service.sincronizar();

        assertThat(resposta.registrosSincronizados()).isEqualTo(2);
        assertThat(resposta.ultimoIdAuditoria()).isEqualTo(12L);
        verify(oracleRepository).findByIdGreaterThanOrderByIdAsc(10L);
    }

    @Test
    void deveConverterJsonDoOracleEmSubdocumento() {
        when(mongoRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(oracleRepository.findByIdGreaterThanOrderByIdAsc(0L)).thenReturn(List.of(
                auditoria(1L, "UPDATE", "{\"id_agendamento\":3,\"status\":\"AGENDADO\"}",
                        "{\"id_agendamento\":3,\"status\":\"CONFIRMADO\"}")
        ));

        service.sincronizar();

        AuditoriaDocument salvo = capturarSalvos().getFirst();
        assertThat(salvo.getId()).isEqualTo(1L);
        assertThat(salvo.getValoresAnteriores()).containsEntry("status", "AGENDADO");
        assertThat(salvo.getValoresNovos()).containsEntry("status", "CONFIRMADO");
        assertThat(salvo.getValoresNovos()).containsEntry("id_agendamento", 3);
    }

    @Test
    void devePreservarConteudoQuandoJsonForInvalido() {
        when(mongoRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(oracleRepository.findByIdGreaterThanOrderByIdAsc(0L)).thenReturn(List.of(
                auditoria(1L, "INSERT", null, "texto que nao e json {")
        ));

        service.sincronizar();

        AuditoriaDocument salvo = capturarSalvos().getFirst();
        assertThat(salvo.getValoresAnteriores()).isNull();
        assertThat(salvo.getValoresNovos()).containsEntry("conteudoOriginal", "texto que nao e json {");
    }

    @Test
    void naoDeveFalharQuandoNaoHaRegistrosNovos() {
        when(mongoRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(documento(20L)));
        when(oracleRepository.findByIdGreaterThanOrderByIdAsc(20L)).thenReturn(List.of());

        SincronizacaoResponse resposta = service.sincronizar();

        assertThat(resposta.registrosSincronizados()).isZero();
        assertThat(resposta.ultimoIdAuditoria()).isEqualTo(20L);
    }

    @SuppressWarnings("unchecked")
    private List<AuditoriaDocument> capturarSalvos() {
        ArgumentCaptor<List<AuditoriaDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(mongoRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private AuditoriaOracle auditoria(Long id, String operacao, String anteriores, String novos) {
        return new AuditoriaOracle(id, "RM566358", operacao, LocalDateTime.now(),
                "ELO_AGENDAMENTO", 9L, anteriores, novos);
    }

    private AuditoriaDocument documento(Long id) {
        return new AuditoriaDocument(id, "RM566358", "INSERT", LocalDateTime.now(),
                "ELO_AGENDAMENTO", 1L, null, null, LocalDateTime.now());
    }
}
