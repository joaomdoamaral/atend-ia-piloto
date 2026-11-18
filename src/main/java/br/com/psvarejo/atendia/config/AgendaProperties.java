package br.com.psvarejo.atendia.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

/**
 * Configuracoes do agendamento via Cal.com.
 * Horario de expediente, dias uteis, duracao e intervalos ficam configurados no proprio Cal.com
 * (Disponibilidade / Limites e intervalos do tipo de evento) — aqui so o que o backend precisa.
 *
 * Com @Validated a aplicacao nem sobe se faltar alguma configuracao obrigatoria (fail fast).
 */
@Validated
@ConfigurationProperties(prefix = "agenda")
public class AgendaProperties {

    /** Chave de API do Cal.com (cal_live_...). */
    @NotBlank(message = "defina CAL_API_KEY")
    private String calApiKey;

    /** URL base da API v2 do Cal.com. */
    @NotBlank
    private String calBaseUrl = "https://api.cal.com/v2";

    /** ID numerico do tipo de evento ("Conversa com Patrick Salomao") no Cal.com. */
    @NotNull(message = "defina CAL_EVENT_TYPE_ID")
    private Long calEventTypeId;

    /** Fuso horario usado para buscar e falar os horarios (ex: America/Sao_Paulo). */
    @NotBlank
    private String timezone = "America/Sao_Paulo";

    /** Quantos horarios livres no maximo devolver por chamada (a IA vai ler em voz alta, entao poucos). */
    @Min(1)
    private int maxSlotsReturned = 5;

    /** Maximo de horarios do mesmo dia, para espalhar as opcoes entre dias diferentes. */
    @Min(1)
    private int maxSlotsPerDay = 2;

    /** Limite de dias a frente que a busca aceita (protecao). */
    @Min(1)
    private int maxDaysAhead = 30;

    /** Segredo compartilhado que a Vapi deve enviar no header X-Vapi-Secret. */
    @NotBlank(message = "defina AGENDA_VAPI_WEBHOOK_SECRET")
    private String vapiWebhookSecret;

    public ZoneId zoneId() {
        return ZoneId.of(timezone);
    }

    public String getCalApiKey() {
        return calApiKey;
    }

    public void setCalApiKey(String calApiKey) {
        this.calApiKey = calApiKey;
    }

    public String getCalBaseUrl() {
        return calBaseUrl;
    }

    public void setCalBaseUrl(String calBaseUrl) {
        this.calBaseUrl = calBaseUrl;
    }

    public Long getCalEventTypeId() {
        return calEventTypeId;
    }

    public void setCalEventTypeId(Long calEventTypeId) {
        this.calEventTypeId = calEventTypeId;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getMaxSlotsReturned() {
        return maxSlotsReturned;
    }

    public void setMaxSlotsReturned(int maxSlotsReturned) {
        this.maxSlotsReturned = maxSlotsReturned;
    }

    public int getMaxSlotsPerDay() {
        return maxSlotsPerDay;
    }

    public void setMaxSlotsPerDay(int maxSlotsPerDay) {
        this.maxSlotsPerDay = maxSlotsPerDay;
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
