package hr.jbrzica.wedding_manager_api.dto;

import java.util.List;

public record TvrtkaResponse(Long id, String naziv, List<ClanTvrtkeResponse> clanovi) {
}
