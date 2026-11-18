package br.com.psvarejo.atendia.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Envelope que a Vapi envia ao chamar uma tool do tipo "function" com server URL:
 * <pre>
 * {"message": {
 *    "type": "tool-calls",
 *    "toolCallList": [{"id": "call_abc", "function": {"name": "check_availability", "arguments": {...}}}],
 *    "call": {"customer": {"number": "+5511999999999"}}
 * }}
 * </pre>
 * So mapeamos o que usamos; o resto e ignorado.
 */
public record VapiToolCallRequest(Message message) {

    public List<ToolCall> toolCalls() {
        if (message == null || message.toolCallList() == null) {
            return List.of();
        }
        return message.toolCallList();
    }

    /** Numero de quem ligou (E.164), quando a ligacao veio de telefone. */
    public String numeroDoCliente() {
        if (message == null || message.call() == null || message.call().customer() == null) {
            return null;
        }
        return message.call().customer().number();
    }

    public record Message(String type, List<ToolCall> toolCallList, Call call) {
    }

    public record ToolCall(String id, Function function) {
    }

    /** "arguments" pode vir como objeto JSON ou como string com JSON dentro — tratamos os dois no controller. */
    public record Function(String name, JsonNode arguments) {
    }

    public record Call(Customer customer) {
    }

    public record Customer(String number) {
    }
}
