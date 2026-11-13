package br.com.psvarejo.atendia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AtendIaPilotoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtendIaPilotoApplication.class, args);
    }
}
