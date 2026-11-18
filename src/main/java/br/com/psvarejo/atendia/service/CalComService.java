package br.com.psvarejo.atendia.service;

import br.com.psvarejo.atendia.calcom.CalComDtos.Attendee;
import br.com.psvarejo.atendia.calcom.CalComDtos.BookingRequest;
import br.com.psvarejo.atendia.calcom.CalComDtos.BookingResponse;
import br.com.psvarejo.atendia.calcom.CalComDtos.Slot;
import br.com.psvarejo.atendia.calcom.CalComDtos.SlotsResponse;
import br.com.psvarejo.atendia.config.AgendaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class CalComService {

    private static final Logger log = LoggerFactory.getLogger(CalComService.class);

    // Cada endpoint da API v2 do Cal.com e versionado por header; sem ele cai numa versao antiga.
    private static final String SLOTS_API_VERSION = "2024-09-04";
    private static final String BOOKINGS_API_VERSION = "2026-02-25";

    private static final int INTERVALO_MINIMO_ENTRE_OPCOES_MIN = 120;

    private final RestClient calComRestClient;
    private final AgendaProperties props;

    public CalComService(RestClient calComRestClient, AgendaProperties props) {
        this.calComRestClient = calComRestClient;
        this.props = props;
    }

    /**
     * Busca horarios livres no Cal.com (que ja considera expediente, agenda do Google, aviso minimo etc.)
     * e escolhe poucas opcoes, espalhadas entre dias, para a IA oferecer por voz.
     */
    public List<OffsetDateTime> buscarHorariosLivres(LocalDate dataInicio, LocalDate dataFim, String preferenciaPeriodo) {
        ZoneId zone = props.zoneId();
        LocalDate hoje = LocalDate.now(zone);
        LocalDate inicio = dataInicio.isBefore(hoje) ? hoje : dataInicio;
        LocalDate limite = hoje.plusDays(props.getMaxDaysAhead());
        LocalDate fim = dataFim.isAfter(limite) ? limite : dataFim;
        if (fim.isBefore(inicio)) {
            return List.of();
        }

        Map<String, List<Slot>> slotsPorDia = consultarSlots(inicio, fim);

        List<OffsetDateTime> escolhidos = new ArrayList<>();
        // TreeMap ordena pelas chaves "yyyy-MM-dd", ou seja, por dia
        for (List<Slot> slotsDoDia : new TreeMap<>(slotsPorDia).values()) {
            int doDia = 0;
            OffsetDateTime ultimoDoDia = null;
            for (Slot slot : slotsDoDia) {
                if (escolhidos.size() >= props.getMaxSlotsReturned()) {
                    return escolhidos;
                }
                if (doDia >= props.getMaxSlotsPerDay()) {
                    break;
                }
                OffsetDateTime horario = OffsetDateTime.parse(slot.start()).atZoneSameInstant(zone).toOffsetDateTime();
                // Por telefone e melhor oferecer opcoes espacadas ("12h ou 15h?") do que 12:00 e 12:15
                boolean pertoDoAnterior = ultimoDoDia != null
                        && horario.isBefore(ultimoDoDia.plusMinutes(INTERVALO_MINIMO_ENTRE_OPCOES_MIN));
                if (!pertoDoAnterior && atendePreferencia(horario, preferenciaPeriodo)) {
                    escolhidos.add(horario);
                    ultimoDoDia = horario;
                    doDia++;
                }
            }
        }
        return escolhidos;
    }

    /**
     * Cria a reserva no Cal.com. O Cal.com revalida a disponibilidade: se o horario
     * acabou de ser ocupado, ele responde 4xx e devolvemos falha para a IA oferecer outro.
     */
    public ResultadoReserva criarReserva(OffsetDateTime inicio, String nome, String empresa, String assunto,
                                         String telefone, String email) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("origem", "atendente-ia");
        if (temValor(empresa)) {
            metadata.put("empresa", empresa);
        }

        // "notes" e o campo "Observacoes adicionais" do Cal.com: aparece no evento do Google Calendar
        BookingRequest request = new BookingRequest(
                inicio.toInstant().toString(),
                props.getCalEventTypeId(),
                new Attendee(nome, props.getTimezone(), telefone, email, "pt-BR"),
                Map.of("notes", montarObservacoes(empresa, assunto, telefone)),
                metadata);

        try {
            BookingResponse resposta = calComRestClient.post()
                    .uri("/bookings")
                    .header("cal-api-version", BOOKINGS_API_VERSION)
                    .body(request)
                    .retrieve()
                    .body(BookingResponse.class);

            String uid = resposta != null && resposta.data() != null ? resposta.data().uid() : null;
            log.info("Reserva criada no Cal.com: uid={} status={}", uid, resposta != null && resposta.data() != null ? resposta.data().status() : null);
            return ResultadoReserva.sucesso(uid);
        } catch (HttpClientErrorException e) {
            log.warn("Cal.com recusou a reserva ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            // Nem toda recusa e conflito: so dizemos "horario ocupado" se o slot realmente sumiu da agenda
            Motivo motivo = horarioAindaLivre(inicio) ? Motivo.ERRO : Motivo.HORARIO_OCUPADO;
            return ResultadoReserva.falha(motivo, e.getResponseBodyAsString());
        }
    }

    static String montarObservacoes(String empresa, String assunto, String telefone) {
        return "Empresa: " + (temValor(empresa) ? empresa.trim() : "não informada")
                + "\nAssunto: " + (temValor(assunto) ? assunto.trim() : "não informado")
                + "\nTelefone: " + (temValor(telefone) ? telefone : "não informado")
                + "\nAgendado pelo atendente virtual (IA).";
    }

    private static boolean temValor(String valor) {
        return valor != null && !valor.isBlank();
    }

    private boolean horarioAindaLivre(OffsetDateTime inicio) {
        LocalDate dia = inicio.atZoneSameInstant(props.zoneId()).toLocalDate();
        return consultarSlots(dia, dia).values().stream()
                .flatMap(List::stream)
                .anyMatch(slot -> OffsetDateTime.parse(slot.start()).isEqual(inicio));
    }

    private Map<String, List<Slot>> consultarSlots(LocalDate inicio, LocalDate fim) {
        ZoneId zone = props.zoneId();
        SlotsResponse resposta = calComRestClient.get()
                .uri(uri -> uri.path("/slots")
                        .queryParam("eventTypeId", props.getCalEventTypeId())
                        .queryParam("start", inicio.atStartOfDay(zone).toInstant())
                        .queryParam("end", fim.plusDays(1).atStartOfDay(zone).toInstant())
                        .queryParam("timeZone", zone.getId())
                        .build())
                .header("cal-api-version", SLOTS_API_VERSION)
                .retrieve()
                .body(SlotsResponse.class);

        if (resposta == null || resposta.data() == null) {
            return Map.of();
        }
        return resposta.data();
    }

    private boolean atendePreferencia(OffsetDateTime horario, String preferenciaPeriodo) {
        if (preferenciaPeriodo == null || preferenciaPeriodo.isBlank()) {
            return true;
        }
        return switch (preferenciaPeriodo.trim().toLowerCase()) {
            case "manha", "manhã" -> horario.getHour() < 12;
            case "tarde" -> horario.getHour() >= 12;
            default -> true;
        };
    }

    public enum Motivo { CONFIRMADA, HORARIO_OCUPADO, ERRO }

    public record ResultadoReserva(Motivo motivo, String reservaId, String detalheErro) {
        public boolean confirmada() {
            return motivo == Motivo.CONFIRMADA;
        }

        static ResultadoReserva sucesso(String reservaId) {
            return new ResultadoReserva(Motivo.CONFIRMADA, reservaId, null);
        }

        static ResultadoReserva falha(Motivo motivo, String detalheErro) {
            return new ResultadoReserva(motivo, null, detalheErro);
        }
    }
}
