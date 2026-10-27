package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.DugResponse;
import hr.jbrzica.wedding_manager_api.entity.Dodatak;
import hr.jbrzica.wedding_manager_api.entity.Paket;
import hr.jbrzica.wedding_manager_api.entity.Uplata;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.repository.UplataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UplataServiceTest {

    @Mock
    private UplataRepository uplataRepository;

    @Mock
    private VjencanjeService vjencanjeService;

    @InjectMocks
    private UplataService uplataService;

    @Test
    void izracunajDug_zbrajaPaketIDodatkeIOduzimaUplate() {
        // PRIPREMA: paket 2500, dron 300, album 450, kapara 1000
        Vjencanje vjencanje = napraviVjencanje("2500.00", "300.00", "450.00");

        Uplata kapara = new Uplata();
        kapara.setIznos(new BigDecimal("1000.00"));

        when(vjencanjeService.dohvatiPoId(1L)).thenReturn(vjencanje);
        when(uplataRepository.findByVjencanjeId(1L)).thenReturn(List.of(kapara));

        // IZVRŠI
        DugResponse rezultat = uplataService.izracunajDug(1L);

        // PROVJERI
        assertEquals(new BigDecimal("3250.00"), rezultat.ukupnaCijena());
        assertEquals(new BigDecimal("1000.00"), rezultat.uplaceno());
        assertEquals(new BigDecimal("2250.00"), rezultat.dug());
    }

    @Test
    void izracunajDug_bezUplata_dugJeCijelaCijena() {
        Vjencanje vjencanje = napraviVjencanje("2500.00");

        when(vjencanjeService.dohvatiPoId(1L)).thenReturn(vjencanje);
        when(uplataRepository.findByVjencanjeId(1L)).thenReturn(List.of());

        DugResponse rezultat = uplataService.izracunajDug(1L);

        assertEquals(new BigDecimal("2500.00"), rezultat.dug());
    }

    // pomoćna metoda: prva cijena je paket, ostale su dodaci
    private Vjencanje napraviVjencanje(String cijenaPaketa, String... cijeneDodataka) {
        Paket paket = new Paket();
        paket.setCijena(new BigDecimal(cijenaPaketa));

        Vjencanje vjencanje = new Vjencanje();
        vjencanje.setPaket(paket);

        for (String cijena : cijeneDodataka) {
            Dodatak dodatak = new Dodatak();
            dodatak.setCijena(new BigDecimal(cijena));
            vjencanje.getDodaci().add(dodatak);
        }
        return vjencanje;
    }
}