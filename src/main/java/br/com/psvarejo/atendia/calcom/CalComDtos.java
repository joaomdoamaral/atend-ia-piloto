package br.com.psvarejo.atendia.calcom;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Formatos de request/response da API v2 do Cal.com usados por este backend.
 * Agrupados aqui porque sao detalhes de integracao, nao fazem parte da nossa API.
 */
public final class CalComDtos {

    private CalComDtos() {
    }

    /** GET /v2/slots -> {"status":"success","data":{"2026-11-18":[{"start":"2026-11-18T09:00:00.000-03:00"}]}} */
    public record SlotsResponse(String status, Map<String, List<Slot>> data) {
    }

    /** "start" fica como String de proposito: o Jackson converteria o offset para UTC. */
    public record Slot(String start) {
    }

    /** POST /v2/bookings */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BookingRequest(String start, Long eventTypeId, Attendee attendee,
                                 Map<String, String> bookingFieldsResponses, Map<String, String> metadata) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Attendee(String name, String timeZone, String phoneNumber, String email, String language) {
    }

    public record BookingResponse(String status, Booking data) {
    }

    public record Booking(Long id, String uid, String status, String start) {
    }
}
