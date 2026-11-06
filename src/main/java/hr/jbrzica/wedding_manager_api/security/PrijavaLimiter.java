package hr.jbrzica.wedding_manager_api.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nakon 5 krivih lozinki za isti email prijava je blokirana 15 minuta, da se lozinka ne može pogađati.
 * Pamti se u memoriji: aplikacija radi kao jedan proces, pa je to dovoljno; restart briše brojanje.
 */
@Component
public class PrijavaLimiter {

    static final int MAX_POKUSAJA = 5;
    static final Duration PROZOR = Duration.ofMinutes(15);

    private final Map<String, List<Instant>> neuspjeli = new ConcurrentHashMap<>();
    private final Clock sat;

    public PrijavaLimiter() {
        this(Clock.systemUTC());
    }

    // Test podmeće svoj sat da ne mora čekati 15 minuta.
    PrijavaLimiter(Clock sat) {
        this.sat = sat;
    }

    public void provjeri(String email) {
        Instant sada = sat.instant();
        List<Instant> pokusaji = svjeziPokusaji(email, sada);
        if (pokusaji.size() >= MAX_POKUSAJA) {
            Instant otkljucava = pokusaji.get(0).plus(PROZOR);
            long minuta = Duration.between(sada, otkljucava).toMinutes() + 1;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Previše neuspjelih prijava. Pokušajte ponovno za " + minuta + " min.");
        }
    }

    public void neuspjeh(String email) {
        Instant sada = sat.instant();
        neuspjeli.compute(email, (kljuc, lista) -> {
            List<Instant> nova = lista == null ? new ArrayList<>() : lista;
            nova.add(sada);
            return nova;
        });
    }

    public void uspjeh(String email) {
        neuspjeli.remove(email);
    }

    private List<Instant> svjeziPokusaji(String email, Instant sada) {
        Instant granica = sada.minus(PROZOR);
        List<Instant> ostali = neuspjeli.computeIfPresent(email, (kljuc, lista) -> {
            lista.removeIf(t -> !t.isAfter(granica));
            return lista.isEmpty() ? null : lista;
        });
        return ostali == null ? List.of() : List.copyOf(ostali);
    }
}
