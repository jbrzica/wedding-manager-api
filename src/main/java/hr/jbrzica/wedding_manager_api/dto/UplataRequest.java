package hr.jbrzica.wedding_manager_api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UplataRequest(
        BigDecimal iznos,
        LocalDate datum,
        String opis
) {
}