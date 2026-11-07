package hr.jbrzica.wedding_manager_api.dto;

// Samo putanje: frontend dodaje adresu servera na kojem radi.
public record SuradnikLinkResponse(String putanja) {

    public static SuradnikLinkResponse za(String token) {
        return new SuradnikLinkResponse("/s/" + token);
    }
}
