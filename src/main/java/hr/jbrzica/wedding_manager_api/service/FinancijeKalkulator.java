package hr.jbrzica.wedding_manager_api.service;

import hr.jbrzica.wedding_manager_api.dto.FinancijeResponse;
import hr.jbrzica.wedding_manager_api.entity.ClanEkipe;
import hr.jbrzica.wedding_manager_api.entity.Dodatak;
import hr.jbrzica.wedding_manager_api.entity.Paket;
import hr.jbrzica.wedding_manager_api.entity.Uplata;
import hr.jbrzica.wedding_manager_api.entity.Vjencanje;
import hr.jbrzica.wedding_manager_api.entity.VrstaUplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;

@Component
public class FinancijeKalkulator {

    public BigDecimal predlozenaCijena(Paket paket, Collection<Dodatak> dodaci) {
        BigDecimal cijena = paket != null ? paket.getCijena() : BigDecimal.ZERO;
        for (Dodatak dodatak : dodaci) {
            cijena = cijena.add(dodatak.getCijena());
        }
        return cijena;
    }

    public FinancijeResponse izracunaj(Vjencanje vjencanje) {
        BigDecimal uplaceno = BigDecimal.ZERO;
        boolean kaparaPlacena = false;
        for (Uplata uplata : vjencanje.getUplate()) {
            uplaceno = uplaceno.add(uplata.getIznos());
            if (uplata.getVrsta() == VrstaUplate.KAPARA) {
                kaparaPlacena = true;
            }
        }

        BigDecimal honorari = BigDecimal.ZERO;
        for (ClanEkipe clan : vjencanje.getEkipa()) {
            if (clan.getHonorar() != null) {
                honorari = honorari.add(clan.getHonorar());
            }
        }

        BigDecimal ostali = vjencanje.getOstaliTroskovi() != null ? vjencanje.getOstaliTroskovi() : BigDecimal.ZERO;
        BigDecimal cijena = vjencanje.getDogovorenaCijena();
        return new FinancijeResponse(
                cijena,
                uplaceno,
                cijena.subtract(uplaceno),
                kaparaPlacena,
                honorari,
                ostali,
                cijena.subtract(honorari).subtract(ostali)
        );
    }
}
