package br.com.psvarejo.atendia.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Resposta no formato que a Vapi espera: {"results": [{"toolCallId": "call_abc", "result": "..."}]}.
 * "result" e texto que o LLM le; em caso de erro usamos "error".
 */
public record VapiToolCallResponse(List<Result> results) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Result(String toolCallId, String result, String error) {

        public static Result ok(String toolCallId, String result) {
            return new Result(toolCallId, result, null);
        }

        public static Result erro(String toolCallId, String error) {
            return new Result(toolCallId, null, error);
        }
    }
}
