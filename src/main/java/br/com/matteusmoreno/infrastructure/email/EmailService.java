package br.com.matteusmoreno.infrastructure.email;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class EmailService {

    private final Mailer mailer;
    private final Template welcomeTemplate;


    public EmailService(
            Mailer mailer,
            @Location("emails/welcome.html") Template welcomeTemplate
    ) {
        this.mailer = mailer;
        this.welcomeTemplate = welcomeTemplate;
    }

    @RunOnVirtualThread
    public void sendWelcomeEmail(String artistEmail, String artistName) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Iniciando envio assíncrono de email de boas-vindas para: {}", artistEmail);

                String htmlContent = welcomeTemplate
                        .data("artistName", artistName)
                        .data("platformName", "PedePlay")
                        .data("supportEmail", "suporte@pedeplay.com.br")
                        .render();

                mailer.send(
                        Mail.withHtml(artistEmail, "🎵 Bem-vindo ao PedePlay!", htmlContent)
                );

                log.info("✅ Email de boas-vindas enviado com sucesso para: {}", artistEmail);
            } catch (Exception e) {
                log.error("❌ Erro ao enviar email de boas-vindas para {}: {}", artistEmail, e.getMessage());
            }
        });
    }
}

