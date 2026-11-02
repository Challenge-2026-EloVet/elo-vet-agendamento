package br.com.fiap.elovetagendamento.clinica.dto;

import java.util.List;

/**
 * Clínica no formato consumido pelo app mobile do tutor (interface Clinic).
 * Os nomes dos campos seguem o contrato do app, em inglês.
 * distance e distanceKm só são preenchidos quando a localização do tutor é informada.
 */
public record ClinicaResponse(
        String id,
        String name,
        String distance,
        Double distanceKm,
        List<String> specialties,
        Double rating,
        String address,
        String phone,
        List<String> accessibility,
        Coordenadas coordinates
) {
    public record Coordenadas(double latitude, double longitude) {
    }
}
