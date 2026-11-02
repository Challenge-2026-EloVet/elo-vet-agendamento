package br.com.fiap.elovetagendamento.clinica.service;

import br.com.fiap.elovetagendamento.clinica.dto.ClinicaResponse;
import br.com.fiap.elovetagendamento.clinica.exception.ClinicaNotFoundException;
import br.com.fiap.elovetagendamento.clinica.exception.LocalizacaoInvalidaException;
import br.com.fiap.elovetagendamento.clinica.repository.ClinicaRepository;
import br.com.fiap.elovetagendamento.clinica.repository.ClinicaRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Monta as clínicas no formato do app do tutor: endereço formatado, especialidades,
 * acessibilidade, coordenadas e, quando a localização do tutor é informada,
 * a distância até cada clínica (fórmula de Haversine).
 */
@Service
public class ClinicaService {

    private static final Logger log = LoggerFactory.getLogger(ClinicaService.class);
    private static final double RAIO_TERRA_KM = 6371.0;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final ClinicaRepository clinicaRepository;

    public ClinicaService(ClinicaRepository clinicaRepository) {
        this.clinicaRepository = clinicaRepository;
    }

    /**
     * Lista as clínicas. Com latitude e longitude, calcula a distância e ordena da mais próxima
     * para a mais distante; sem localização, ordena por nome.
     * Com especialidade, devolve apenas as clínicas que a atendem (sem diferenciar acentos e maiúsculas).
     */
    public List<ClinicaResponse> listar(Double latitude, Double longitude, String especialidade) {
        validarLocalizacao(latitude, longitude);
        Map<Long, List<String>> especialidades = clinicaRepository.especialidadesPorClinica();
        Map<Long, List<String>> acessibilidade = clinicaRepository.acessibilidadePorClinica();

        List<ClinicaResponse> clinicas = clinicaRepository.listar().stream()
                .filter(clinica -> especialidade == null || especialidade.isBlank()
                        || atende(especialidades.getOrDefault(clinica.id(), List.of()), especialidade))
                .map(clinica -> montar(clinica, especialidades, acessibilidade, latitude, longitude))
                .sorted(latitude != null
                        ? Comparator.comparing(ClinicaResponse::distanceKm, Comparator.nullsLast(Comparator.naturalOrder()))
                        : Comparator.comparing(ClinicaResponse::name))
                .toList();

        log.info("Consulta de clínicas (localização={}, especialidade={}): {} resultado(s)",
                latitude != null ? "informada" : "não informada", especialidade, clinicas.size());
        return clinicas;
    }

    public ClinicaResponse buscar(Long idClinica, Double latitude, Double longitude) {
        validarLocalizacao(latitude, longitude);
        ClinicaRow clinica = clinicaRepository.buscar(idClinica)
                .orElseThrow(() -> new ClinicaNotFoundException(idClinica));
        return montar(clinica, clinicaRepository.especialidadesPorClinica(),
                clinicaRepository.acessibilidadePorClinica(), latitude, longitude);
    }

    private ClinicaResponse montar(ClinicaRow clinica, Map<Long, List<String>> especialidades,
                                   Map<Long, List<String>> acessibilidade, Double latitude, Double longitude) {
        boolean temCoordenadas = clinica.latitude() != null && clinica.longitude() != null;
        Double distanciaKm = temCoordenadas && latitude != null
                ? arredondar(distanciaKm(latitude, longitude, clinica.latitude(), clinica.longitude()))
                : null;

        return new ClinicaResponse(
                String.valueOf(clinica.id()),
                clinica.nome(),
                distanciaKm != null ? formatarDistancia(distanciaKm) : null,
                distanciaKm,
                especialidades.getOrDefault(clinica.id(), List.of()),
                clinica.avaliacao(),
                formatarEndereco(clinica),
                formatarTelefone(clinica.telefone()),
                acessibilidade.getOrDefault(clinica.id(), List.of()),
                temCoordenadas ? new ClinicaResponse.Coordenadas(clinica.latitude(), clinica.longitude()) : null
        );
    }

    /** Distância em linha reta entre dois pontos, pela fórmula de Haversine. */
    static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * RAIO_TERRA_KM * Math.asin(Math.sqrt(a));
    }

    /** Até 1 km em metros ("850 m"); acima disso em km com uma casa decimal ("2,4 km"). */
    static String formatarDistancia(double km) {
        if (km < 1) {
            return Math.round(km * 1000 / 10.0) * 10 + " m";
        }
        return String.format(PT_BR, "%.1f km", km);
    }

    static String formatarTelefone(String telefone) {
        if (telefone == null) return null;
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() == 10) {
            return "(%s) %s-%s".formatted(digitos.substring(0, 2), digitos.substring(2, 6), digitos.substring(6));
        }
        if (digitos.length() == 11) {
            return "(%s) %s-%s".formatted(digitos.substring(0, 2), digitos.substring(2, 7), digitos.substring(7));
        }
        return telefone;
    }

    static String formatarEndereco(ClinicaRow clinica) {
        String complemento = clinica.complemento() != null ? " - " + clinica.complemento() : "";
        return "%s, %s%s - %s, %s - %s".formatted(clinica.logradouro(), clinica.numero(), complemento,
                clinica.bairro(), clinica.cidade(), clinica.estado());
    }

    private static boolean atende(List<String> especialidades, String procurada) {
        String alvo = normalizar(procurada);
        return especialidades.stream().anyMatch(especialidade -> normalizar(especialidade).equals(alvo));
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(PT_BR);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100) / 100.0;
    }

    private static void validarLocalizacao(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new LocalizacaoInvalidaException("Informe latitude e longitude juntas");
        }
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            throw new LocalizacaoInvalidaException("Coordenadas fora do intervalo válido (latitude -90 a 90, longitude -180 a 180)");
        }
    }
}
