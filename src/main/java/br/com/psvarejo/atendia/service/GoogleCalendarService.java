package br.com.psvarejo.atendia.service;

import br.com.psvarejo.atendia.config.AgendaProperties;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;
import com.google.api.services.calendar.model.TimePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);

    private final Calendar calendar;
    private final AgendaProperties agendaProperties;

    public GoogleCalendarService(Calendar calendar, AgendaProperties agendaProperties) {
        this.calendar = calendar;
        this.agendaProperties = agendaProperties;
    }

    /**
     * Calcula horarios livres dentro do range informado, respeitando expediente,
     * dias uteis configurados e a preferencia de periodo (manha/tarde), consultando
     * o FreeBusy real do calendario do consultor.
     */
    public List<ZonedDateTime> buscarHorariosLivres(LocalDate dataInicio, LocalDate dataFim, String preferenciaPeriodo) throws IOException {
        ZoneId zoneId = ZoneId.of(agendaProperties.getTimezone());

        LocalDate limiteMaximo = LocalDate.now(zoneId).plusDays(agendaProperties.getMaxDaysAhead());
        LocalDate fimEfetivo = dataFim.isAfter(limiteMaximo) ? limiteMaximo : dataFim;

        ZonedDateTime inicioJanela = dataInicio.atStartOfDay(zoneId);
        ZonedDateTime fimJanela = fimEfetivo.plusDays(1).atStartOfDay(zoneId);

        List<TimePeriod> ocupados = consultarFreeBusy(inicioJanela, fimJanela, zoneId);

        List<ZonedDateTime> disponiveis = new ArrayList<>();
        ZonedDateTime agora = ZonedDateTime.now(zoneId);
        int duracaoMinutos = agendaProperties.getSlotDurationMinutes();

        for (LocalDate dia = dataInicio; !dia.isAfter(fimEfetivo) && disponiveis.size() < agendaProperties.getMaxSlotsReturned(); dia = dia.plusDays(1)) {
            if (!agendaProperties.getWorkingDays().contains(dia.getDayOfWeek())) {
                continue;
            }

            ZonedDateTime cursor = LocalDateTime.of(dia, agendaProperties.getBusinessHourStart()).atZone(zoneId);
            ZonedDateTime fimExpediente = LocalDateTime.of(dia, agendaProperties.getBusinessHourEnd()).atZone(zoneId);

            while (!cursor.plusMinutes(duracaoMinutos).isAfter(fimExpediente) && disponiveis.size() < agendaProperties.getMaxSlotsReturned()) {
                ZonedDateTime fimSlot = cursor.plusMinutes(duracaoMinutos);

                boolean noPassado = cursor.isBefore(agora);
                boolean foraDaPreferencia = !atendePreferencia(cursor, preferenciaPeriodo);
                boolean conflita = conflitaComOcupados(cursor, fimSlot, ocupados);

                if (!noPassado && !foraDaPreferencia && !conflita) {
                    disponiveis.add(cursor);
                }

                cursor = fimSlot;
            }
        }

        return disponiveis;
    }

    /**
     * Confirma se um slot especifico ainda esta livre e, se estiver, cria o evento.
     * Retorna o id do evento criado, ou Optional vazio se o horario acabou de ser ocupado
     * (condicao de corrida entre a consulta de disponibilidade e a confirmacao).
     */
    public String criarEventoSeDisponivel(ZonedDateTime inicio, String nome, String empresa, String contato) throws IOException {
        ZoneId zoneId = ZoneId.of(agendaProperties.getTimezone());
        ZonedDateTime fim = inicio.plusMinutes(agendaProperties.getSlotDurationMinutes());

        List<TimePeriod> ocupados = consultarFreeBusy(inicio, fim, zoneId);
        if (conflitaComOcupados(inicio, fim, ocupados)) {
            return null;
        }

        String descricao = "Lead capturado pelo atendente de IA.\nContato: " + valorOuNaoInformado(contato)
                + "\nEmpresa: " + valorOuNaoInformado(empresa);

        Event evento = new Event()
                .setSummary("Conversa PS Varejo - " + nome)
                .setDescription(descricao)
                .setStart(toEventDateTime(inicio))
                .setEnd(toEventDateTime(fim));

        Event criado = calendar.events()
                .insert(agendaProperties.getCalendarId(), evento)
                .execute();

        log.info("Evento criado no Google Calendar: {}", criado.getId());
        return criado.getId();
    }

    private List<TimePeriod> consultarFreeBusy(ZonedDateTime inicio, ZonedDateTime fim, ZoneId zoneId) throws IOException {
        FreeBusyRequest request = new FreeBusyRequest()
                .setTimeMin(new DateTime(inicio.toInstant().toEpochMilli()))
                .setTimeMax(new DateTime(fim.toInstant().toEpochMilli()))
                .setTimeZone(zoneId.getId())
                .setItems(List.of(new FreeBusyRequestItem().setId(agendaProperties.getCalendarId())));

        FreeBusyResponse response = calendar.freebusy().query(request).execute();
        var calendarInfo = response.getCalendars().get(agendaProperties.getCalendarId());
        if (calendarInfo == null || calendarInfo.getBusy() == null) {
            return List.of();
        }
        return calendarInfo.getBusy();
    }

    private boolean conflitaComOcupados(ZonedDateTime inicioSlot, ZonedDateTime fimSlot, List<TimePeriod> ocupados) {
        long inicioMillis = inicioSlot.toInstant().toEpochMilli();
        long fimMillis = fimSlot.toInstant().toEpochMilli();

        for (TimePeriod periodo : ocupados) {
            long ocupadoInicio = periodo.getStart().getValue();
            long ocupadoFim = periodo.getEnd().getValue();
            boolean seSobrepoe = inicioMillis < ocupadoFim && fimMillis > ocupadoInicio;
            if (seSobrepoe) {
                return true;
            }
        }
        return false;
    }

    private boolean atendePreferencia(ZonedDateTime horario, String preferenciaPeriodo) {
        if (preferenciaPeriodo == null || preferenciaPeriodo.isBlank()) {
            return true;
        }
        String preferencia = preferenciaPeriodo.trim().toLowerCase();
        int hora = horario.getHour();
        return switch (preferencia) {
            case "manha", "manhã" -> hora < 12;
            case "tarde" -> hora >= 12;
            default -> true;
        };
    }

    private EventDateTime toEventDateTime(ZonedDateTime zonedDateTime) {
        return new EventDateTime()
                .setDateTime(new DateTime(zonedDateTime.toInstant().toEpochMilli()))
                .setTimeZone(zonedDateTime.getZone().getId());
    }

    private String valorOuNaoInformado(String valor) {
        return (valor == null || valor.isBlank()) ? "nao informado" : valor;
    }
}
