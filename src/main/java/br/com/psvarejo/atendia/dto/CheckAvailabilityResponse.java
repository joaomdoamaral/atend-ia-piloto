package br.com.psvarejo.atendia.dto;

import java.util.List;

public record CheckAvailabilityResponse(
        List<String> horariosDisponiveis
) {
}
