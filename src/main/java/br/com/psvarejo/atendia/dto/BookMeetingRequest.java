package br.com.psvarejo.atendia.dto;

import jakarta.validation.constraints.NotBlank;

public record BookMeetingRequest(
        @NotBlank String horarioEscolhido,
        @NotBlank String nome,
        String empresa,
        String contato
) {
}
