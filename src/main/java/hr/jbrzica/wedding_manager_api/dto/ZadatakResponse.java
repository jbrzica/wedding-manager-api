package hr.jbrzica.wedding_manager_api.dto;

import hr.jbrzica.wedding_manager_api.entity.Zadatak;

public record ZadatakResponse(Long id, String naziv, boolean gotovo) {

    public static ZadatakResponse iz(Zadatak z) {
        return new ZadatakResponse(z.getId(), z.getNaziv(), z.isGotovo());
    }
}
