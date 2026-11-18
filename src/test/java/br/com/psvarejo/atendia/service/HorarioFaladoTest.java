package br.com.psvarejo.atendia.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HorarioFaladoTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 17); // terca-feira

    @ParameterizedTest
    @CsvSource({
            "8, 0, às oito da manhã",
            "9, 30, às nove e meia da manhã",
            "11, 15, às onze e quinze da manhã",
            "12, 0, ao meio-dia",
            "13, 0, à uma da tarde",
            "13, 30, à uma e meia da tarde",
            "14, 0, às duas da tarde",
            "17, 45, às cinco e quarenta e cinco da tarde",
            "18, 0, às seis da noite"
    })
    void falaHorasComoAoTelefone(int hora, int minuto, String esperado) {
        assertThat(HorarioFalado.hora(hora, minuto)).isEqualTo(esperado);
    }

    @Test
    void usaAmanhaParaODiaSeguinte() {
        var horario = ZonedDateTime.of(2026, 11, 18, 14, 0, 0, 0, SP);
        assertThat(HorarioFalado.falar(horario, HOJE)).isEqualTo("amanhã, quarta-feira, dia 18, às duas da tarde");
    }

    @Test
    void naoRepeteOMesQuandoEOMesmo() {
        assertThat(HorarioFalado.dia(LocalDate.of(2026, 11, 20), HOJE)).isEqualTo("sexta-feira, dia 20");
    }

    @Test
    void falaOMesQuandoMuda() {
        assertThat(HorarioFalado.dia(LocalDate.of(2026, 12, 1), HOJE)).isEqualTo("terça-feira, dia 1 de dezembro");
    }
}
