package hr.jbrzica.wedding_manager_api.exception;

import java.util.Map;

public record ErrorResponse(
        int status,
        String poruka,
        Map<String, String> greske
) {
}