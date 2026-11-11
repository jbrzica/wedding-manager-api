package hr.jbrzica.wedding_manager_api.ugovor;

import java.time.Instant;

// vlastiti = false znači da tvrtka koristi zadani predložak iz aplikacije
public record PredlozakInfo(boolean vlastiti, String nazivDatoteke, Instant azurirano) {
}
