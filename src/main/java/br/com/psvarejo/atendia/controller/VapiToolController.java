package br.com.psvarejo.atendia.controller;

import br.com.psvarejo.atendia.config.AgendaProperties;
import br.com.psvarejo.atendia.dto.ToolArgs;
import br.com.psvarejo.atendia.dto.VapiToolCallRequest;
import br.com.psvarejo.atendia.dto.VapiToolCallRequest.ToolCall;
import br.com.psvarejo.atendia.dto.VapiToolCallResponse;
import br.com.psvarejo.atendia.dto.VapiToolCallResponse.Result;
import br.com.psvarejo.atendia.service.CalComService;
import br.com.psvarejo.atendia.service.CalComService.ResultadoReserva;
import br.com.psvarejo.atendia.service.HorarioFalado;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endpoints chamados pelas tools (function calling) do assistente na Vapi.
 * Recebem o envelope da Vapi e respondem no formato {"results":[{"toolCallId","result"}]}.
 * O texto de "result" e lido pelo LLM, por isso as mensagens sao instrucoes em portugues.
 */
@RestController
@RequestMapping("/vapi/tools")
public class VapiToolController {

    private static final Logger log = LoggerFactory.getLogger(VapiToolController.class);

    private final CalComService calComService;
    private final AgendaProperties props;
    private final ObjectMapper objectMapper;

    public VapiToolController(CalComService calComService, AgendaProperties props, ObjectMapper objectMapper) {
        this.calComService = calComService;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/check-availability")
    public VapiToolCallResponse checkAvailability(@RequestBody VapiToolCallRequest request) {
        return processar(request, toolCall -> {
            var args = lerArgumentos(toolCall, ToolArgs.CheckAvailability.class);
            LocalDate hoje = LocalDate.now(props.zoneId());
            LocalDate inicio = args.dataInicio() == null || args.dataInicio().isBlank()
                    ? hoje : parseData(args.dataInicio(), "dataInicio");
            LocalDate fim = args.dataFim() == null || args.dataFim().isBlank()
                    ? inicio.plusDays(6) : parseData(args.dataFim(), "dataFim");

            log.info("check_availability: {} a {} (preferencia={})", inicio, fim, args.preferenciaPeriodo());
            List<OffsetDateTime> horarios = calComService.buscarHorariosLivres(inicio, fim, args.preferenciaPeriodo());

            if (horarios.isEmpty()) {
                return "Nao ha horarios livres entre " + inicio + " e " + fim
                        + ". Pergunte ao cliente se pode ser em outro periodo e consulte novamente.";
            }

            List<Map<String, String>> opcoes = new ArrayList<>();
            for (OffsetDateTime h : horarios) {
                Map<String, String> opcao = new LinkedHashMap<>();
                opcao.put("horario", h.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                opcao.put("falar", HorarioFalado.falar(h, props.zoneId(), hoje));
                opcoes.add(opcao);
            }
            return objectMapper.writeValueAsString(Map.of(
                    "horariosDisponiveis", opcoes,
                    "instrucao", "Ofereca estas opcoes usando o campo 'falar'. Ao agendar, envie o campo 'horario' exatamente como esta."));
        });
    }

    @PostMapping("/book-meeting")
    public VapiToolCallResponse bookMeeting(@RequestBody VapiToolCallRequest request) {
        return processar(request, toolCall -> {
            var args = lerArgumentos(toolCall, ToolArgs.BookMeeting.class);
            if (args.nome() == null || args.nome().isBlank()) {
                throw new ArgumentoInvalidoException("Falta o nome do cliente. Pergunte o nome antes de agendar.");
            }
            if (args.horarioEscolhido() == null || args.horarioEscolhido().isBlank()) {
                throw new ArgumentoInvalidoException("Falta o horarioEscolhido. Use um dos horarios devolvidos por check_availability.");
            }
            OffsetDateTime horario;
            try {
                horario = OffsetDateTime.parse(args.horarioEscolhido());
            } catch (DateTimeParseException e) {
                throw new ArgumentoInvalidoException("horarioEscolhido invalido. Envie exatamente o campo 'horario' devolvido por check_availability.");
            }

            // Se o LLM nao coletou o telefone, usa o numero de quem ligou
            String telefone = normalizarTelefone(args.telefone());
            if (telefone == null) {
                telefone = normalizarTelefone(request.numeroDoCliente());
            }
            String email = args.email() == null || args.email().isBlank() ? null : args.email().trim();
            // O Cal.com exige telefone ou e-mail. Em ligacao pelo navegador nao existe "numero de quem ligou".
            if (telefone == null && email == null) {
                throw new ArgumentoInvalidoException("Falta um telefone de contato valido. Pergunte o telefone com DDD "
                        + "(ex: 11 98765-4321), confirme repetindo o numero e chame book_meeting de novo com o campo telefone.");
            }

            log.info("book_meeting: horario={} nome={} empresa={} assunto={}", horario, args.nome(), args.empresa(), args.assunto());
            ResultadoReserva resultado = calComService.criarReserva(horario, args.nome().trim(), args.empresa(), args.assunto(), telefone, email);

            if (resultado.confirmada()) {
                return "Reuniao confirmada para " + HorarioFalado.falar(horario, props.zoneId(), LocalDate.now(props.zoneId()))
                        + ". Confirme com o cliente e diga que o Patrick Salomao vai conversar com ele nesse horario.";
            }
            if (resultado.motivo() == CalComService.Motivo.HORARIO_OCUPADO) {
                return "Esse horario acabou de ser ocupado. Peca desculpas, chame check_availability de novo e ofereca outro horario.";
            }
            throw new IllegalStateException("Cal.com recusou a reserva: " + resultado.detalheErro());
        });
    }

    private VapiToolCallResponse processar(VapiToolCallRequest request, ToolHandler handler) {
        List<Result> results = new ArrayList<>();
        for (ToolCall toolCall : request.toolCalls()) {
            try {
                results.add(Result.ok(toolCall.id(), handler.executar(toolCall)));
            } catch (ArgumentoInvalidoException e) {
                log.warn("Argumento invalido na tool {}: {}", toolCall.id(), e.getMessage());
                results.add(Result.erro(toolCall.id(), e.getMessage()));
            } catch (Exception e) {
                log.error("Falha ao executar a tool {}", toolCall.id(), e);
                results.add(Result.erro(toolCall.id(),
                        "Falha temporaria ao acessar a agenda. Peca desculpas e ofereca registrar um recado para o Patrick retornar."));
            }
        }
        return new VapiToolCallResponse(results);
    }

    /** Aceita "arguments" como objeto JSON ou como string contendo JSON. */
    private <T> T lerArgumentos(ToolCall toolCall, Class<T> tipo) throws Exception {
        JsonNode args = toolCall.function() == null ? null : toolCall.function().arguments();
        if (args == null || args.isNull()) {
            args = objectMapper.createObjectNode();
        } else if (args.isTextual()) {
            args = objectMapper.readTree(args.asText());
        }
        return objectMapper.treeToValue(args, tipo);
    }

    private LocalDate parseData(String valor, String campo) {
        try {
            return LocalDate.parse(valor.trim().substring(0, Math.min(10, valor.trim().length())));
        } catch (DateTimeParseException e) {
            throw new ArgumentoInvalidoException(campo + " invalido. Use o formato AAAA-MM-DD, ex: 2026-11-18.");
        }
    }

    /** Converte para E.164 (+55...). Devolve null se nao parecer um telefone valido. */
    static String normalizarTelefone(String telefone) {
        if (telefone == null) {
            return null;
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (telefone.trim().startsWith("+") && digitos.length() >= 10) {
            return "+" + digitos;
        }
        if (digitos.length() == 10 || digitos.length() == 11) {
            return "+55" + digitos; // DDD + numero
        }
        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) {
            return "+" + digitos;
        }
        return null;
    }

    @FunctionalInterface
    private interface ToolHandler {
        String executar(ToolCall toolCall) throws Exception;
    }

    private static class ArgumentoInvalidoException extends RuntimeException {
        ArgumentoInvalidoException(String message) {
            super(message);
        }
    }
}
