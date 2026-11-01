package br.com.fiap.elovetagendamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EloVetAgendamentoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EloVetAgendamentoApplication.class, args);
    }

}
