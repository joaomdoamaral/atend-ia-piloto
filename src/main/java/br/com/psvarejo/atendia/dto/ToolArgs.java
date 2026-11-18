package br.com.psvarejo.atendia.dto;

/**
 * Argumentos que o LLM preenche ao chamar cada tool (definidos no schema da tool na Vapi).
 * Datas chegam como texto e sao validadas no controller, para devolver uma mensagem amigavel ao LLM.
 */
public final class ToolArgs {

    private ToolArgs() {
    }

    /** check_availability: datas yyyy-MM-dd; preferenciaPeriodo "manha", "tarde" ou vazio. */
    public record CheckAvailability(String dataInicio, String dataFim, String preferenciaPeriodo) {
    }

    /** book_meeting: horarioEscolhido e um dos valores ISO devolvidos por check_availability. */
    public record BookMeeting(String horarioEscolhido, String nome, String empresa, String assunto, String telefone, String email) {
    }
}
