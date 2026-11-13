package br.com.psvarejo.atendia.dto;

public record BookMeetingResponse(
        boolean confirmado,
        String mensagem,
        String eventoId
) {
    public static BookMeetingResponse sucesso(String eventoId) {
        return new BookMeetingResponse(true, "Reuniao agendada com sucesso.", eventoId);
    }

    public static BookMeetingResponse conflito() {
        return new BookMeetingResponse(false,
                "Esse horario acabou de ficar indisponivel. Peca para o cliente escolher outro horario.", null);
    }
}
