package br.com.psvarejo.atendia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Configuracoes do comportamento de agendamento.
 * Todos os valores tem default e podem ser sobrescritos via application.yml
 * ou variavel de ambiente (ex: AGENDA_SLOT_DURATION_MINUTES).
 */
@ConfigurationProperties(prefix = "agenda")
public class AgendaProperties {

    /** E-mail do calendario do consultor (geralmente o proprio e-mail do Google Workspace dele). */
    private String calendarId;

    /** Caminho absoluto para o JSON da service account do Google. */
    private String credentialsPath;

    /** Fuso horario usado para calcular os horarios (ex: America/Sao_Paulo). */
    private String timezone = "America/Sao_Paulo";

    /** Horario de inicio do expediente considerado para oferecer horarios. */
    private LocalTime businessHourStart = LocalTime.of(9, 0);

    /** Horario de fim do expediente. */
    private LocalTime businessHourEnd = LocalTime.of(18, 0);

    /** Dias da semana validos para oferecer horario (default: seg a sex). */
    private List<DayOfWeek> workingDays = List.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    /** Duracao de cada reuniao / slot, em minutos. */
    private int slotDurationMinutes = 30;

    /** Quantos horarios livres no maximo retornar por chamada. */
    private int maxSlotsReturned = 5;

    /** Limite maximo de dias a frente que a busca de disponibilidade aceita (protecao). */
    private int maxDaysAhead = 30;

    /** Segredo compartilhado que a Vapi deve enviar no header X-Vapi-Secret. */
    private String vapiWebhookSecret;

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public String getCredentialsPath() {
        return credentialsPath;
    }

    public void setCredentialsPath(String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public LocalTime getBusinessHourStart() {
        return businessHourStart;
    }

    public void setBusinessHourStart(LocalTime businessHourStart) {
        this.businessHourStart = businessHourStart;
    }

    public LocalTime getBusinessHourEnd() {
        return businessHourEnd;
    }

    public void setBusinessHourEnd(LocalTime businessHourEnd) {
        this.businessHourEnd = businessHourEnd;
    }

    public List<DayOfWeek> getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(List<DayOfWeek> workingDays) {
        this.workingDays = workingDays;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public void setSlotDurationMinutes(int slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public int getMaxSlotsReturned() {
        return maxSlotsReturned;
    }

    public void setMaxSlotsReturned(int maxSlotsReturned) {
        this.maxSlotsReturned = maxSlotsReturned;
    }

    public int getMaxDaysAhead() {
        return maxDaysAhead;
    }

    public void setMaxDaysAhead(int maxDaysAhead) {
        this.maxDaysAhead = maxDaysAhead;
    }

    public String getVapiWebhookSecret() {
        return vapiWebhookSecret;
    }

    public void setVapiWebhookSecret(String vapiWebhookSecret) {
        this.vapiWebhookSecret = vapiWebhookSecret;
    }
}
