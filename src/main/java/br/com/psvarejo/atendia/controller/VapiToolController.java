package br.com.psvarejo.atendia.controller;

import br.com.psvarejo.atendia.dto.BookMeetingRequest;
import br.com.psvarejo.atendia.dto.BookMeetingResponse;
import br.com.psvarejo.atendia.dto.CheckAvailabilityRequest;
import br.com.psvarejo.atendia.dto.CheckAvailabilityResponse;
import br.com.psvarejo.atendia.service.GoogleCalendarService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Endpoints chamados pelas "tools" (function calling) configuradas no assistente da Vapi.
 * Veja o README do projeto para o passo a passo de como registrar essas duas tools
 * no dashboard da Vapi apontando para /vapi/tools/check-availability e /vapi/tools/book-meeting.
 */
@RestController
@RequestMapping("/vapi/tools")
public class VapiToolController {

    private static final Logger log = LoggerFactory.getLogger(VapiToolController.class);

    private final GoogleCalendarService googleCalendarService;

    public VapiToolController(GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
    }

    @PostMapping("/check-availability")
    public ResponseEntity<CheckAvailabilityResponse> checkAvailability(@Valid @RequestBody CheckAvailabilityRequest request) throws IOException {
        log.info("check-availability: de {} ate {} (preferencia={})", request.dataInicio(), request.dataFim(), request.preferenciaPeriodo());

        List<ZonedDateTime> horarios = googleCalendarService.buscarHorariosLivres(
                request.dataInicio(), request.dataFim(), request.preferenciaPeriodo());

        List<String> horariosIso = horarios.stream()
                .map(h -> h.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .toList();

        return ResponseEntity.ok(new CheckAvailabilityResponse(horariosIso));
    }

    @PostMapping("/book-meeting")
    public ResponseEntity<BookMeetingResponse> bookMeeting(@Valid @RequestBody BookMeetingRequest request) throws IOException {
        log.info("book-meeting: horario={} nome={}", request.horarioEscolhido(), request.nome());

        ZonedDateTime horario = OffsetDateTime.parse(request.horarioEscolhido()).toZonedDateTime();

        String eventoId = googleCalendarService.criarEventoSeDisponivel(
                horario, request.nome(), request.empresa(), request.contato());

        if (eventoId == null) {
            return ResponseEntity.ok(BookMeetingResponse.conflito());
        }
        return ResponseEntity.ok(BookMeetingResponse.sucesso(eventoId));
    }
}
