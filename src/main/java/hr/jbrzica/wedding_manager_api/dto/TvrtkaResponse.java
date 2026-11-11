package hr.jbrzica.wedding_manager_api.dto;

import java.util.List;

public record TvrtkaResponse(Long id, String naziv, String adresa, String oib, String telefon, String email,
                             List<ClanTvrtkeResponse> clanovi) {
}
