package br.com.psvarejo.atendia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class CalComClientConfig {

    /**
     * RestClient (Spring 6.1+) ja apontando para a API do Cal.com e autenticado com a chave.
     * O RestClient.Builder injetado vem do Spring Boot, ja com o ObjectMapper da aplicacao.
     */
    @Bean
    public RestClient calComRestClient(RestClient.Builder builder, AgendaProperties props) {
        return builder
                .baseUrl(props.getCalBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getCalApiKey())
                .build();
    }
}
