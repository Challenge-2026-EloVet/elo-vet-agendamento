package br.com.fiap.elovetagendamento.auditoria.service;

import br.com.fiap.elovetagendamento.auditoria.dto.SincronizacaoResponse;
import br.com.fiap.elovetagendamento.procedure.service.PkgEloVetService;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaSincronizacaoServiceTest {

    private static final LocalDate ONTEM = LocalDate.of(2026, 10, 31);
    private static final String COLLECTION = AuditoriaSincronizacaoService.COLLECTION;

    private static final String JSON_DA_FUNCTION = """
            [{"id_auditoria":15,"tipo_operacao":"INSERT","data_hora_operacao":"31/10/2026 14:22:10",\
            "usuario_banco":"RM566358","id_agendamento":9,\
            "valores_novos":{"id_agendamento":9,"id_pet":3,"id_veterinario":5,"id_clinica":4,"status":"AGENDADO","data_hora":"01/11/2026 14:00"},\
            "pet":{"id_pet":3,"nome":"Thor","especie":"Cachorro","raca":"Dachshund","sexo":"M","data_nascimento":null,"idade_aproximada":4,"castrado":"SIM"},\
            "responsavel":{"id_responsavel":2,"nome_completo":"Rafael Martins","telefone":"11988887777"},\
            "veterinario":{"id_veterinario":5,"nome_completo":"Joao Pedro Salles","crmv":"CRMV-SP 11345","telefone":"11976540005"},\
            "clinica":{"id_clinica":4,"nome":"Elo Vet Santo Amaro","cnpj":"45678901000163","telefone":"1130454455","email":"contato@elovet-santoamaro.com.br","bairro":"Santo Amaro","cidade":"Sao Paulo","estado":"SP"}},\
            {"id_auditoria":16,"tipo_operacao":"UPDATE","data_hora_operacao":"31/10/2026 15:00:00",\
            "usuario_banco":"RM566358","id_agendamento":9,\
            "valores_novos":{"id_agendamento":9,"id_pet":3,"id_veterinario":5,"id_clinica":4,"status":"CONFIRMADO","data_hora":"01/11/2026 14:00"},\
            "pet":null,"responsavel":null,"veterinario":null,"clinica":null}]""";

    @Mock
    private PkgEloVetService pkgEloVetService;

    @Mock
    private MongoTemplate mongoTemplate;

    private AuditoriaSincronizacaoService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaSincronizacaoService(pkgEloVetService, mongoTemplate);
        when(pkgEloVetService.dataReferenciaExportacao()).thenReturn(ONTEM);
    }

    @Test
    void deveGravarCadaObjetoDoJsonComoDocumentoPreservandoOsObjetosAninhados() {
        when(pkgEloVetService.exportaValoresNovos()).thenReturn(JSON_DA_FUNCTION);
        when(mongoTemplate.remove(any(Query.class), eq(COLLECTION))).thenReturn(DeleteResult.acknowledged(0));

        SincronizacaoResponse resposta = service.sincronizar();

        List<Document> gravados = capturarGravados();
        assertThat(resposta.documentosGravados()).isEqualTo(2);
        assertThat(gravados).extracting(documento -> documento.get("_id")).containsExactly(15, 16);

        Document primeiro = gravados.getFirst();
        assertThat(primeiro.getString("dataReferencia")).isEqualTo("2026-10-31");
        assertThat(primeiro.get("exportadoEm")).isNotNull();
        assertThat(primeiro.get("valores_novos", Document.class).getString("status")).isEqualTo("AGENDADO");
        assertThat(primeiro.get("pet", Document.class).getString("raca")).isEqualTo("Dachshund");
        assertThat(primeiro.get("clinica", Document.class).getString("cidade")).isEqualTo("Sao Paulo");
        assertThat(gravados.getLast().get("pet")).isNull();
    }

    @Test
    void deveSubstituirAExportacaoAnteriorDoMesmoDia() {
        when(pkgEloVetService.exportaValoresNovos()).thenReturn(JSON_DA_FUNCTION);
        when(mongoTemplate.remove(any(Query.class), eq(COLLECTION))).thenReturn(DeleteResult.acknowledged(2));

        SincronizacaoResponse resposta = service.sincronizar();

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(captor.capture(), eq(COLLECTION));
        assertThat(captor.getValue().getQueryObject().getString("dataReferencia")).isEqualTo("2026-10-31");
        assertThat(resposta.documentosSubstituidos()).isEqualTo(2L);
    }

    @Test
    void naoDeveInserirNadaQuandoNaoHaAuditoriaNoDiaAnterior() {
        when(pkgEloVetService.exportaValoresNovos()).thenReturn("[]");
        when(mongoTemplate.remove(any(Query.class), eq(COLLECTION))).thenReturn(DeleteResult.acknowledged(0));

        SincronizacaoResponse resposta = service.sincronizar();

        assertThat(resposta.documentosGravados()).isZero();
        verify(mongoTemplate, never()).insert(anyCollection(), anyString());
    }

    @Test
    void deveFalharSemApagarNadaQuandoAFunctionRetornaNulo() {
        when(pkgEloVetService.exportaValoresNovos()).thenReturn(null);

        assertThatThrownBy(() -> service.sincronizar()).isInstanceOf(ExportacaoInvalidaException.class);
        verify(mongoTemplate, never()).remove(any(Query.class), anyString());
    }

    @Test
    void deveFalharSemApagarNadaQuandoOJsonEInvalido() {
        when(pkgEloVetService.exportaValoresNovos()).thenReturn("[{\"id_auditoria\": 1,");

        assertThatThrownBy(() -> service.sincronizar()).isInstanceOf(ExportacaoInvalidaException.class);
        verify(mongoTemplate, never()).remove(any(Query.class), anyString());
    }

    @SuppressWarnings("unchecked")
    private List<Document> capturarGravados() {
        ArgumentCaptor<Collection<Document>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(mongoTemplate).insert(captor.capture(), eq(COLLECTION));
        return new ArrayList<>(captor.getValue());
    }
}
