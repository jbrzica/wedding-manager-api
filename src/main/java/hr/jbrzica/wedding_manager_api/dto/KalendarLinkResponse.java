package hr.jbrzica.wedding_manager_api.dto;

// Samo putanja: frontend dodaje adresu servera na kojem radi.
public record KalendarLinkResponse(String putanja) {

    public static KalendarLinkResponse za(String token) {
        return new KalendarLinkResponse("/api/ical/" + token + ".ics");
    }
}
