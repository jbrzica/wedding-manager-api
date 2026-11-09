package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.UplataRequest;
import hr.jbrzica.wedding_manager_api.entity.StatusVjencanja;
import hr.jbrzica.wedding_manager_api.entity.Uplata;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.entity.VrstaUplate;
import hr.jbrzica.wedding_manager_api.repository.UplataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UplataServiceTest {

    @Mock
    private UplataRepository uplataRepository;

    @Mock
    private VjencanjeService vjencanjeService;

    @Mock
    PovijestServis povijest;

    @InjectMocks
    private UplataService uplataService;

    @Test
    void kapara_potvrdujeVjencanjeKojeJeUpit() {
        Vjencanje vjencanje = new Vjencanje();   // novo vjenčanje je UPIT
        when(vjencanjeService.dohvatiEntitet(1L)).thenReturn(vjencanje);
        when(uplataRepository.save(any(Uplata.class))).thenAnswer(poziv -> poziv.getArgument(0));

        uplataService.stvori(1L, uplata("500.00", VrstaUplate.KAPARA));

        assertEquals(StatusVjencanja.POTVRDENO, vjencanje.getStatus());
        assertEquals(1, vjencanje.getUplate().size());
    }

    @Test
    void rata_neMijenjaStatus() {
        Vjencanje vjencanje = new Vjencanje();
        when(vjencanjeService.dohvatiEntitet(1L)).thenReturn(vjencanje);
        when(uplataRepository.save(any(Uplata.class))).thenAnswer(poziv -> poziv.getArgument(0));

        uplataService.stvori(1L, uplata("500.00", VrstaUplate.RATA));

        assertEquals(StatusVjencanja.UPIT, vjencanje.getStatus());
    }

    private UplataRequest uplata(String iznos, VrstaUplate vrsta) {
        return new UplataRequest(new BigDecimal(iznos), LocalDate.of(2026, 11, 5), vrsta, null);
    }
}
