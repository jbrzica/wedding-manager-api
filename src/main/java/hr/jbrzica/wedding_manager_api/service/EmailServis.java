package hr.jbrzica.wedding_manager_api.service;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Lokalno šalje preko SMTP-a (Mailpit). Na serveru, kad je postavljen Brevo API ključ,
 * šalje preko Brevo HTTP API-ja, jer Railway na Hobby planu blokira SMTP portove.
 */
@Service
public class EmailServis {

    private final JavaMailSender mailSender;
    private final String posiljatelj;
    private final String brevoKljuc;
    private final RestClient brevo = RestClient.create("https://api.brevo.com/v3");

    public EmailServis(JavaMailSender mailSender,
                       @Value("${app.mail.posiljatelj}") String posiljatelj,
                       @Value("${app.mail.brevo-api-kljuc:}") String brevoKljuc) {
        this.mailSender = mailSender;
        this.posiljatelj = posiljatelj;
        this.brevoKljuc = brevoKljuc;
    }

    public void posalji(String prima, String naslov, String tekst) {
        if (brevoKljuc.isBlank()) {
            posaljiPrekoSmtp(prima, naslov, tekst);
        } else {
            posaljiPrekoBreva(prima, naslov, tekst);
        }
    }

    private void posaljiPrekoSmtp(String prima, String naslov, String tekst) {
        SimpleMailMessage poruka = new SimpleMailMessage();
        poruka.setFrom(posiljatelj);
        poruka.setTo(prima);
        poruka.setSubject(naslov);
        poruka.setText(tekst);
        mailSender.send(poruka);
    }

    private void posaljiPrekoBreva(String prima, String naslov, String tekst) {
        InternetAddress od;
        try {
            // "Wedding Manager <noreply@...>" -> ime i adresa zasebno, kako Brevo traži
            od = new InternetAddress(posiljatelj);
        } catch (AddressException e) {
            throw new MailSendException("Neispravna adresa pošiljatelja: " + posiljatelj, e);
        }
        String ime = od.getPersonal() != null ? od.getPersonal() : "Wedding Manager";

        Map<String, Object> tijelo = Map.of(
                "sender", Map.of("name", ime, "email", od.getAddress()),
                "to", List.of(Map.of("email", prima)),
                "subject", naslov,
                "textContent", tekst
        );

        try {
            brevo.post()
                    .uri("/smtp/email")
                    .header("api-key", brevoKljuc)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(tijelo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // MailException -> GlobalExceptionHandler vraća 503 "Mail se trenutno ne može poslati"
            throw new MailSendException("Brevo nije poslao mail", e);
        }
    }
}
