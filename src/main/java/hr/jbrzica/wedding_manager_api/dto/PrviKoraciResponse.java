package hr.jbrzica.wedding_manager_api.dto;

// Što je nova tvrtka već napravila, za listu „Prvi koraci“ na početnoj stranici
public record PrviKoraciResponse(boolean paketi, boolean suradnici, boolean vjencanje, boolean kalendar, long brojVjencanja) {
}
