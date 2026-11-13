package br.com.psvarejo.atendia.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Payload que a Vapi envia ao chamar a tool "check_availability".
 * preferenciaPeriodo aceita "manha", "tarde" ou null/vazio (qualquer horario).
 */
public record CheckAvailabilityRequest(
        @NotNull LocalDate dataInicio,
        @NotNull LocalDate dataFim,
        String preferenciaPeriodo
) {
}
