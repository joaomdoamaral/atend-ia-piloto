package br.com.psvarejo.atendia.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Converte um horario no jeito que uma pessoa fala ao telefone, para o TTS ler com naturalidade.
 * Ex: 2026-11-18T14:00 -> "amanhã, quarta-feira, dia 18, às duas da tarde"
 * (em vez de "18/11 às 14:00", que o TTS le como "dezoito barra onze as quatorze horas").
 */
public final class HorarioFalado {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private static final String[] HORAS = {
            "meia-noite", "uma", "duas", "três", "quatro", "cinco", "seis", "sete", "oito", "nove", "dez", "onze", "meio-dia"
    };

    private HorarioFalado() {
    }

    public static String falar(ZonedDateTime horario, LocalDate hoje) {
        return dia(horario.toLocalDate(), hoje) + ", " + hora(horario.getHour(), horario.getMinute());
    }

    public static String falar(OffsetDateTime horario, ZoneId zone, LocalDate hoje) {
        return falar(horario.atZoneSameInstant(zone), hoje);
    }

    static String dia(LocalDate data, LocalDate hoje) {
        String diaSemana = data.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
        long distancia = ChronoUnit.DAYS.between(hoje, data);
        String base = diaSemana + ", dia " + data.getDayOfMonth();
        if (distancia == 0) {
            return "hoje, " + base;
        }
        if (distancia == 1) {
            return "amanhã, " + base;
        }
        // Fora da semana corrente, o dia do mes evita ambiguidade; o mes so quando muda
        if (data.getMonth() != hoje.getMonth()) {
            base += " de " + data.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        }
        return base;
    }

    static String hora(int hora, int minuto) {
        String complemento = switch (minuto) {
            case 0 -> "";
            case 15 -> " e quinze";
            case 30 -> " e meia";
            case 45 -> " e quarenta e cinco";
            default -> " e " + minuto;
        };

        if (hora == 0) {
            return "à meia-noite" + complemento;
        }
        if (hora == 12) {
            return "ao meio-dia" + complemento;
        }

        int hora12 = hora > 12 ? hora - 12 : hora;
        String periodo = hora < 12 ? "da manhã" : hora < 18 ? "da tarde" : "da noite";
        String artigo = hora12 == 1 ? "à" : "às";
        return artigo + " " + HORAS[hora12] + complemento + " " + periodo;
    }
}
