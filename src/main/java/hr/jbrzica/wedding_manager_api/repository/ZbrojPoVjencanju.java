package hr.jbrzica.wedding_manager_api.repository;

import java.math.BigDecimal;

// Zbroj iznosa (uplata ili honorara) za jedno vjenčanje, iz GROUP BY upita
public record ZbrojPoVjencanju(Long vjencanjeId, BigDecimal iznos) {
}
