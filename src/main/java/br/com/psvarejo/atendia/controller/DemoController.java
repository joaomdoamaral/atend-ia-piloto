package br.com.psvarejo.atendia.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Configuracao da pagina de demonstracao (static/index.html), que permite conversar
 * com o assistente pelo navegador usando o SDK web da Vapi.
 * A Public Key da Vapi e feita para ficar exposta no navegador; a Private Key nunca passa por aqui.
 */
@RestController
public class DemoController {

    private final String vapiPublicKey;
    private final String vapiAssistantId;

    public DemoController(@Value("${demo.vapi-public-key:}") String vapiPublicKey,
                          @Value("${demo.vapi-assistant-id:}") String vapiAssistantId) {
        this.vapiPublicKey = vapiPublicKey;
        this.vapiAssistantId = vapiAssistantId;
    }

    @GetMapping("/demo/config")
    public ResponseEntity<Map<String, String>> config() {
        if (vapiPublicKey.isBlank() || vapiAssistantId.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("erro", "Demo nao configurada: defina VAPI_PUBLIC_KEY e VAPI_ASSISTANT_ID"));
        }
        return ResponseEntity.ok(Map.of("publicKey", vapiPublicKey, "assistantId", vapiAssistantId));
    }
}
