package br.com.psvarejo.atendia.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Configuration
public class GoogleCalendarConfig {

    private final AgendaProperties agendaProperties;

    public GoogleCalendarConfig(AgendaProperties agendaProperties) {
        this.agendaProperties = agendaProperties;
    }

    @Bean
    public Calendar googleCalendarClient() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials;
        try (FileInputStream credentialsStream = new FileInputStream(agendaProperties.getCredentialsPath())) {
            credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(List.of(CalendarScopes.CALENDAR));
        }

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        )
                .setApplicationName("atend-ia-piloto")
                .build();
    }
}
