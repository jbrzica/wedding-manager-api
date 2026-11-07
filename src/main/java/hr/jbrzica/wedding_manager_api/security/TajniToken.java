package hr.jbrzica.wedding_manager_api.security;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Nasumičan token za javne linkove (kalendar, stranica suradnika).
 * 32 bajta iz SecureRandom ne mogu se pogoditi; Base64 bez znakova koji smetaju u adresi.
 */
public final class TajniToken {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TajniToken() {
    }

    public static String novi() {
        byte[] bajtovi = new byte[32];
        RANDOM.nextBytes(bajtovi);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bajtovi);
    }
}
