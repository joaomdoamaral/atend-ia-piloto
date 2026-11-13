package br.com.psvarejo.atendia.security;

import br.com.psvarejo.atendia.config.AgendaProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Valida que a chamada aos endpoints /vapi/tools/** veio realmente da Vapi,
 * conferindo um segredo compartilhado enviado no header X-Vapi-Secret.
 * Configure esse mesmo valor no "Server URL Secret" (ou header customizado)
 * da definicao da tool dentro do dashboard da Vapi.
 */
@Component
public class VapiWebhookAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Vapi-Secret";

    private final AgendaProperties agendaProperties;

    public VapiWebhookAuthFilter(AgendaProperties agendaProperties) {
        this.agendaProperties = agendaProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/vapi/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String segredoConfigurado = agendaProperties.getVapiWebhookSecret();
        String segredoRecebido = request.getHeader(HEADER_NAME);

        boolean segredoAusenteNaConfig = segredoConfigurado == null || segredoConfigurado.isBlank();
        boolean segredoValido = !segredoAusenteNaConfig && segredoConfigurado.equals(segredoRecebido);

        if (!segredoValido) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"erro\":\"nao autorizado\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
