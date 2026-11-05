package hr.jbrzica.wedding_manager_api.repository;

import java.math.BigDecimal;

public record ZbrojUplata(Long vjencanjeId, BigDecimal iznos) {
}
