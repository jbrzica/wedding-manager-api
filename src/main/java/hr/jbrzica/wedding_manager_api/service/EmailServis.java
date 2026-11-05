package hr.jbrzica.wedding_manager_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServis {

    private final JavaMailSender mailSender;
    private final String posiljatelj;

    public EmailServis(JavaMailSender mailSender, @Value("${app.mail.posiljatelj}") String posiljatelj) {
        this.mailSender = mailSender;
        this.posiljatelj = posiljatelj;
    }

    public void posalji(String prima, String naslov, String tekst) {
        SimpleMailMessage poruka = new SimpleMailMessage();
        poruka.setFrom(posiljatelj);
        poruka.setTo(prima);
        poruka.setSubject(naslov);
        poruka.setText(tekst);
        mailSender.send(poruka);
    }
}
