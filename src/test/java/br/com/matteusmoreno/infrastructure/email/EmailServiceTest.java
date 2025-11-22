package br.com.matteusmoreno.infrastructure.email;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * Testes para o EmailService.
 *
 * Para testar sem enviar emails reais, configure no application.properties:
 * %test.quarkus.mailer.mock=true
 */
@QuarkusTest
class EmailServiceTest {

    @Inject
    EmailService emailService;

    @Test
    void testSendWelcomeEmail() {
        // Simula envio de email de boas-vindas
        emailService.sendWelcomeEmail("teste@exemplo.com", "João Silva");

        // Com mock=true, o email será exibido no console
        // Sem mock, o email será enviado de verdade
    }

    @Test
    void testSendSubscriptionLimitWarning() {
        emailService.sendSubscriptionLimitWarning(
                "teste@exemplo.com",
                "João Silva",
                "eventos",
                8,
                10,
                "FREE"
        );
    }

    @Test
    void testSendSubscriptionLimitReached() {
        emailService.sendSubscriptionLimitReached(
                "teste@exemplo.com",
                "João Silva",
                "eventos",
                "FREE"
        );
    }
}

