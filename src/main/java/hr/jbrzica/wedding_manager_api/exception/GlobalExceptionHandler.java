package hr.jbrzica.wedding_manager_api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 i ostale greške koje bacamo u servisima
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status, ex.getReason(), null));
    }

    // 400 - neispravni podaci (@Valid nije prošao)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> greske = new HashMap<>();
        for (FieldError greska : ex.getBindingResult().getFieldErrors()) {
            greske.put(greska.getField(), greska.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Neispravni podaci", greske));
    }

    // 400 - neispravan JSON ili nepostojeća vrijednost enuma (npr. "vrsta": "KAPRA")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNeispravanJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Neispravan JSON ili neispravna vrijednost u zahtjevu", null));
    }

    // 400 - neispravna vrijednost parametra u adresi (npr. ?status=NESTO)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Neispravna vrijednost parametra: " + ex.getName(), null));
    }

    // 400 - nedostaje obavezan parametar u adresi
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Nedostaje parametar: " + ex.getParameterName(), null));
    }

    // 409 - baza odbila zapis, npr. brisanje paketa koji se koristi na vjenčanju
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Zapis se ne može spremiti ili obrisati jer krši pravila baze (npr. još se negdje koristi)", null));
    }
}
