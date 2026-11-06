package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Osoba;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OsobaDto(
        @NotBlank(message = "Ime je obavezno") String ime,
        @NotBlank(message = "Prezime je obavezno") String prezime,
        String telefon,
        @Email(message = "Email nije ispravan") String email,
        String adresa,
        @Pattern(regexp = "\\d{11}", message = "OIB mora imati točno 11 znamenki") String oib
) {

    public static OsobaDto iz(Osoba osoba) {
        if (osoba == null) {
            return null;
        }
        return new OsobaDto(osoba.getIme(), osoba.getPrezime(), osoba.getTelefon(),
                osoba.getEmail(), osoba.getAdresa(), osoba.getOib());
    }

    public Osoba uEntitet() {
        Osoba osoba = new Osoba();
        osoba.setIme(ime);
        osoba.setPrezime(prezime);
        osoba.setTelefon(telefon);
        osoba.setEmail(email);
        osoba.setAdresa(adresa);
        osoba.setOib(oib);
        return osoba;
    }
}
