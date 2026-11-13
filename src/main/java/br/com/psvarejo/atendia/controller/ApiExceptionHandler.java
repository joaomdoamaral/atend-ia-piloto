package br.com.psvarejo.atendia.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleGoogleApiError(IOException ex) {
        log.error("Falha ao comunicar com o Google Calendar", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("erro", "Nao foi possivel consultar a agenda agora. Tente novamente em instantes."));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleDataInvalida(DateTimeParseException ex) {
        log.warn("Data/horario invalido recebido: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("erro", "Formato de data/horario invalido. Use ISO-8601 com offset, ex: 2026-11-14T09:00:00-03:00"));
    }
}
