package hr.jbrzica.wedding_manager_api.service;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
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
        posalji(prima, naslov, tekst, null, null);
    }

    /**
     * @param imePosiljatelja ime koje primatelj vidi (npr. naziv studija); null = zadano iz postavki
     * @param odgovorNa       kamo ide "Odgovori" (npr. mail studija ili klijenta); null = nigdje posebno
     */
    public void posalji(String prima, String naslov, String tekst, String imePosiljatelja, String odgovorNa) {
        InternetAddress od;
        try {
            // "Wedding Manager <noreply@...>" -> ime i adresa zasebno
            od = new InternetAddress(posiljatelj);
        } catch (AddressException e) {
            throw new MailSendException("Neispravna adresa pošiljatelja: " + posiljatelj, e);
        }
        String ime = imePosiljatelja != null && !imePosiljatelja.isBlank() ? imePosiljatelja.strip()
                : od.getPersonal() != null ? od.getPersonal() : "Wedding Manager";
        String odgovor = odgovorNa != null && !odgovorNa.isBlank() ? odgovorNa.strip() : null;
        if (brevoKljuc.isBlank()) {
            posaljiPrekoSmtp(prima, naslov, tekst, ime, od.getAddress(), odgovor);
        } else {
            posaljiPrekoBreva(prima, naslov, tekst, ime, od.getAddress(), odgovor);
        }
    }

    private void posaljiPrekoSmtp(String prima, String naslov, String tekst, String ime, String adresa, String odgovor) {
        try {
            jakarta.mail.internet.MimeMessage poruka = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper h =
                    new org.springframework.mail.javamail.MimeMessageHelper(poruka, true, "UTF-8");
            h.setFrom(adresa, ime);
            h.setTo(prima);
            if (odgovor != null) {
                h.setReplyTo(odgovor);
            }
            h.setSubject(naslov);
            h.setText(tekst, html(tekst));
            poruka.setHeader("Content-Language", "hr");
            mailSender.send(poruka);
        } catch (jakarta.mail.MessagingException | java.io.UnsupportedEncodingException e) {
            throw new MailSendException("Mail nije složen", e);
        }
    }

    // HTML verzija s lang="hr": inače Gmail kratke hrvatske mailove zna proglasiti engleskima
    static String html(String tekst) {
        String sigurno = org.springframework.web.util.HtmlUtils.htmlEscape(tekst, "UTF-8");
        String sLinkovima = sigurno.replaceAll("(https?://[^\\s<]+)", "<a href=\"$1\">$1</a>");
        return "<!doctype html><html lang=\"hr\"><head><meta charset=\"utf-8\"></head>"
                + "<body style=\"font-family:Arial,sans-serif;font-size:15px;line-height:1.5;color:#222\">"
                + sLinkovima.replace("\n", "<br>\n") + "</body></html>";
    }

    private void posaljiPrekoBreva(String prima, String naslov, String tekst, String ime, String adresa, String odgovor) {
        Map<String, Object> tijelo = new java.util.LinkedHashMap<>();
        tijelo.put("sender", Map.of("name", ime, "email", adresa));
        tijelo.put("to", List.of(Map.of("email", prima)));
        tijelo.put("subject", naslov);
        tijelo.put("textContent", tekst);
        tijelo.put("htmlContent", html(tekst));
        tijelo.put("headers", Map.of("Content-Language", "hr"));
        if (odgovor != null) {
            tijelo.put("replyTo", Map.of("email", odgovor));
        }

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
