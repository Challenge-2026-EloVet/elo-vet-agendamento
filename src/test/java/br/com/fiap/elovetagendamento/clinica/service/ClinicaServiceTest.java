package br.com.fiap.elovetagendamento.clinica.service;

import br.com.fiap.elovetagendamento.clinica.dto.ClinicaResponse;
import br.com.fiap.elovetagendamento.clinica.exception.ClinicaNotFoundException;
import br.com.fiap.elovetagendamento.clinica.exception.LocalizacaoInvalidaException;
import br.com.fiap.elovetagendamento.clinica.repository.ClinicaRepository;
import br.com.fiap.elovetagendamento.clinica.repository.ClinicaRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClinicaServiceTest {

    // Tutor na Av. Paulista (MASP)
    private static final double LAT_TUTOR = -23.561414;
    private static final double LON_TUTOR = -46.655881;

    @Mock
    private ClinicaRepository clinicaRepository;

    private ClinicaService clinicaService;

    private final ClinicaRow pinheiros = new ClinicaRow(1L, "Clínica Pinheiros", "1130451122", 4.8,
            "Rua dos Pinheiros", "500", null, "Pinheiros", "São Paulo", "SP", -23.566870, -46.683930);
    private final ClinicaRow moema = new ClinicaRow(2L, "Clínica Moema", "11987654321", 4.6,
            "Av. Ibirapuera", "2000", "Sala 12", "Moema", "São Paulo", "SP", -23.601270, -46.664460);
    private final ClinicaRow semCoordenadas = new ClinicaRow(3L, "Clínica Antiga", "1133334444", null,
            "Rua A", "1", null, "Centro", "São Paulo", "SP", null, null);

    @BeforeEach
    void setUp() {
        clinicaService = new ClinicaService(clinicaRepository);
        when(clinicaRepository.listar()).thenReturn(List.of(semCoordenadas, moema, pinheiros));
        when(clinicaRepository.especialidadesPorClinica()).thenReturn(Map.of(
                1L, List.of("Clínica Geral", "Dermatologia"),
                2L, List.of("Ortopedia")));
        when(clinicaRepository.acessibilidadePorClinica()).thenReturn(Map.of(
                1L, List.of("Banheiro adaptado", "Rampa de acesso")));
    }

    @Test
    void listaSemLocalizacaoOrdenadaPorNomeESemDistancia() {
        List<ClinicaResponse> clinicas = clinicaService.listar(null, null, null);

        assertThat(clinicas).extracting(ClinicaResponse::name)
                .containsExactly("Clínica Antiga", "Clínica Moema", "Clínica Pinheiros");
        assertThat(clinicas).allSatisfy(c -> {
            assertThat(c.distance()).isNull();
            assertThat(c.distanceKm()).isNull();
        });
    }

    @Test
    void comLocalizacaoOrdenaDaMaisProximaEDeixaSemCoordenadasNoFim() {
        List<ClinicaResponse> clinicas = clinicaService.listar(LAT_TUTOR, LON_TUTOR, null);

        assertThat(clinicas).extracting(ClinicaResponse::id).containsExactly("1", "2", "3");
        ClinicaResponse maisProxima = clinicas.get(0);
        assertThat(maisProxima.distanceKm()).isBetween(2.8, 3.1);
        assertThat(maisProxima.distance()).isEqualTo("2,9 km");
        assertThat(clinicas.get(2).distanceKm()).isNull();
        assertThat(clinicas.get(2).coordinates()).isNull();
    }

    @Test
    void montaOsCamposNoFormatoDoApp() {
        ClinicaResponse clinica = clinicaService.listar(null, null, null).get(2);

        assertThat(clinica.id()).isEqualTo("1");
        assertThat(clinica.rating()).isEqualTo(4.8);
        assertThat(clinica.phone()).isEqualTo("(11) 3045-1122");
        assertThat(clinica.address()).isEqualTo("Rua dos Pinheiros, 500 - Pinheiros, São Paulo - SP");
        assertThat(clinica.specialties()).containsExactly("Clínica Geral", "Dermatologia");
        assertThat(clinica.accessibility()).containsExactly("Banheiro adaptado", "Rampa de acesso");
        assertThat(clinica.coordinates()).isEqualTo(new ClinicaResponse.Coordenadas(-23.566870, -46.683930));
    }

    @Test
    void clinicaSemEspecialidadeOuAcessibilidadeDevolveListasVazias() {
        ClinicaResponse antiga = clinicaService.listar(null, null, null).get(0);

        assertThat(antiga.specialties()).isEmpty();
        assertThat(antiga.accessibility()).isEmpty();
    }

    @Test
    void filtraPorEspecialidadeSemDiferenciarAcentosEMaiusculas() {
        assertThat(clinicaService.listar(null, null, "clinica geral"))
                .extracting(ClinicaResponse::id).containsExactly("1");
        assertThat(clinicaService.listar(null, null, "Cardiologia")).isEmpty();
    }

    @Test
    void latitudeSemLongitudeEhRecusada() {
        assertThatThrownBy(() -> clinicaService.listar(LAT_TUTOR, null, null))
                .isInstanceOf(LocalizacaoInvalidaException.class);
    }

    @Test
    void coordenadaForaDoIntervaloEhRecusada() {
        assertThatThrownBy(() -> clinicaService.listar(-95.0, LON_TUTOR, null))
                .isInstanceOf(LocalizacaoInvalidaException.class);
    }

    @Test
    void buscaClinicaInexistenteLancaNaoEncontrada() {
        when(clinicaRepository.buscar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicaService.buscar(99L, null, null))
                .isInstanceOf(ClinicaNotFoundException.class);
    }

    @Test
    void formataDistanciaEmMetrosAbaixoDeUmKm() {
        assertThat(ClinicaService.formatarDistancia(0.853)).isEqualTo("850 m");
        assertThat(ClinicaService.formatarDistancia(12.34)).isEqualTo("12,3 km");
    }

    @Test
    void formataTelefoneComOnzeDigitos() {
        assertThat(ClinicaService.formatarTelefone("11987654321")).isEqualTo("(11) 98765-4321");
        assertThat(ClinicaService.formatarTelefone("+55 abc")).isEqualTo("+55 abc");
    }
}
