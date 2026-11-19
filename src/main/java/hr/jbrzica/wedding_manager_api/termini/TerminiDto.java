package hr.jbrzica.wedding_manager_api.termini;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Svi zahtjevi i odgovori za termine na jednom mjestu, jer se koriste samo u ovom paketu
public final class TerminiDto {

    private TerminiDto() {
    }

    // ---------- fotograf ----------

    public record DogadjajRequest(
            @NotBlank(message = "Naziv je obavezan") @Size(max = 200, message = "Naziv je predug") String naziv,
            @Size(max = 2000, message = "Opis je predug") String opis,
            @Size(max = 255, message = "Lokacija je preduga") String lokacija,
            @NotNull(message = "Razmak između termina je obavezan")
            @Min(value = 5, message = "Razmak mora biti barem 5 minuta")
            @Max(value = 240, message = "Razmak može biti najviše 240 minuta") Integer korakMinuta,
            boolean otvoren,
            @Size(max = 40, message = "Kratki link može imati najviše 40 znakova")
            @Pattern(regexp = "^$|^[a-z0-9]([a-z0-9-]*[a-z0-9])?$",
                    message = "Kratki link smije imati samo mala slova bez kvačica, brojke i crticu (npr. bozicno)")
            String kratkiLink,
            @Size(max = 50, message = "Telefon može imati najviše 50 znakova")
            String kontaktTelefon,
            @NotEmpty(message = "Dodaj barem jedan paket") List<@Valid PaketDto> paketi,
            @NotEmpty(message = "Dodaj barem jedan dan") List<@Valid DanDto> dani
    ) {
    }

    public record PaketDto(
            Long id,
            @NotBlank(message = "Naziv paketa je obavezan") @Size(max = 100, message = "Naziv paketa je predug") String naziv,
            @Size(max = 500, message = "Opis paketa je predug") String opis,
            @NotNull(message = "Trajanje je obavezno") @Min(value = 1, message = "Trajanje mora biti barem 1 minuta") Integer trajanjeMinuta,
            @NotNull(message = "Cijena je obavezna") @PositiveOrZero(message = "Cijena ne smije biti negativna") BigDecimal cijena
    ) {
    }

    public record DanDto(
            @NotNull(message = "Datum je obavezan") LocalDate datum,
            @NotNull(message = "Početak je obavezan") LocalTime od,
            @NotNull(message = "Kraj je obavezan") LocalTime doVremena
    ) {
    }

    public record DogadjajSazetak(Long id, String naziv, LocalDate prviDan, LocalDate zadnjiDan, boolean otvoren,
                                  int ukupnoTermina, int zauzeto, BigDecimal prihod) {
    }

    public record RezervacijaResponse(Long id, LocalDate datum, LocalTime vrijeme, Long paketId, String nazivPaketa,
                                      int trajanjeMinuta, BigDecimal cijena, String ime, String telefon, String email,
                                      String napomena, boolean rucno, Instant kreirano) {
    }

    // Jedno mjesto u rasporedu: slobodno (rezervacija = null) ili zauzeto
    public record Mjesto(LocalTime vrijeme, RezervacijaResponse rezervacija) {
    }

    public record DanRaspored(LocalDate datum, LocalTime od, LocalTime doVremena, List<Mjesto> mjesta) {
    }

    public record BrojPoPaketu(String naziv, long broj) {
    }

    public record DogadjajDetalji(Long id, String naziv, String opis, String lokacija, int korakMinuta, boolean otvoren,
                                  String javniKod, String kratkiLink, String kontaktTelefon, int slikaVerzija, List<PaketDto> paketi, List<DanRaspored> raspored,
                                  int ukupnoTermina, int zauzeto, BigDecimal prihod, List<BrojPoPaketu> poPaketima) {
    }

    public record RucnaRezervacijaRequest(
            @NotNull(message = "Paket je obavezan") Long paketId,
            @NotNull(message = "Datum je obavezan") LocalDate datum,
            @NotNull(message = "Vrijeme je obavezno") LocalTime vrijeme,
            @NotBlank(message = "Ime je obavezno") @Size(max = 200, message = "Ime je predugo") String ime,
            @Size(max = 50, message = "Telefon je predug") String telefon,
            @Email(message = "Email nije ispravan") @Size(max = 255, message = "Email je predug") String email,
            @Size(max = 1000, message = "Napomena je preduga") String napomena
    ) {
    }

    public record NovaRezervacija(Long id, Long dogadjajId, String dogadjaj, LocalDate datum, LocalTime vrijeme,
                                  String ime, String nazivPaketa, java.time.Instant kreirano) {
    }

    public record NoveRezervacije(int broj, List<NovaRezervacija> rezervacije) {
    }

    // ---------- javno ----------

    public record JavniPaket(Long id, String naziv, String opis, int trajanjeMinuta, BigDecimal cijena) {
    }

    // Javno se vide samo slobodna vremena, nikad tko je rezervirao
    public record JavniDan(LocalDate datum, int ukupno, List<LocalTime> slobodno, List<LocalTime> zauzeto) {
    }

    public record JavniDogadjaj(String naziv, String opis, String lokacija, String tvrtka, String telefonTvrtke,
                                boolean otvoren, int korakMinuta, int otkazSati, int slikaVerzija, List<JavniPaket> paketi, List<JavniDan> dani) {
    }

    public record JavnaRezervacijaRequest(
            @NotNull(message = "Odaberi paket") Long paketId,
            @NotNull(message = "Odaberi dan") LocalDate datum,
            @NotNull(message = "Odaberi vrijeme") LocalTime vrijeme,
            @NotBlank(message = "Upiši ime i prezime") @Size(max = 200, message = "Ime je predugo") String ime,
            @NotBlank(message = "Upiši broj telefona") @Size(max = 50, message = "Telefon je predug") String telefon,
            @NotBlank(message = "Upiši email za potvrdu") @Email(message = "Email nije ispravan")
            @Size(max = 255, message = "Email je predug") String email,
            @Size(max = 1000, message = "Napomena je preduga") String napomena,
            // Skriveno polje: ljudi ga ne vide, botovi ga popune
            String web
    ) {
    }

    public record PotvrdaRezervacije(String dogadjaj, String tvrtka, String telefonTvrtke, String lokacija,
                                     LocalDate datum, LocalTime vrijeme, String nazivPaketa, int trajanjeMinuta,
                                     BigDecimal cijena, String ime, boolean otkazano, boolean moguceOtkazati,
                                     boolean terminProsao, int otkazSati) {
    }
}
