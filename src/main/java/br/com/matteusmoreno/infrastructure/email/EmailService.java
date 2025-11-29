package br.com.matteusmoreno.infrastructure.email;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class EmailService {

    private final ReactiveMailer mailer;
    private final Template welcomeTemplate;


    public EmailService(
            ReactiveMailer mailer,
            @Location("emails/welcome.html") Template welcomeTemplate
    ) {
        this.mailer = mailer;
        this.welcomeTemplate = welcomeTemplate;
    }

    public Uni<Void> sendWelcomeEmail(String artistEmail, String artistName) {
        log.info("Iniciando envio de email de boas-vindas para: {}", artistEmail);

        String htmlContent = welcomeTemplate
                .data("artistName", artistName)
                .data("platformName", "PedePlay")
                .data("supportEmail", "suporte@pedeplay.com.br")
                .render();

        return mailer.send(
                Mail.withHtml(artistEmail, "Bem-vindo ao PedePlay!", htmlContent)
        )
        .onItem().invoke(() -> log.info("✅ Email de boas-vindas enviado com sucesso para: {}", artistEmail))
        .onFailure().invoke(e -> log.error("❌ Erro ao enviar email de boas-vindas para {}: {}", artistEmail, e.getMessage()));
    }
}

