package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.ClanEkipe;
import hr.jbrzica.wedding_manager_api.entity.UlogaSuradnika;

import java.math.BigDecimal;

public record ClanEkipeResponse(Long suradnikId, String ime, UlogaSuradnika uloga,
                                String telefon, BigDecimal honorar) {

    public static ClanEkipeResponse iz(ClanEkipe c) {
        return new ClanEkipeResponse(c.getSuradnik().getId(), c.getSuradnik().getIme(),
                c.getUloga(), c.getSuradnik().getTelefon(), c.getHonorar());
    }
}
