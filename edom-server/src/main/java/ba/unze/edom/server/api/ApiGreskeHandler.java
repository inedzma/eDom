package ba.unze.edom.server.api;

import ba.unze.edom.server.exception.KorisnikException;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import ba.unze.edom.server.exception.PrijavaException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "ba.unze.edom.server.api")
public class ApiGreskeHandler {

    @ExceptionHandler({PrijavaException.class, NevalidnaPrijavaException.class, KorisnikException.class})
    public ResponseEntity<Map<String, String>> poslovnaGreska(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("poruka", e.getMessage()));
    }

    /** @Valid nije prošao — vrati prvu poruku iz anotacija (@NotBlank i sl.). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> nevalidanZahtjev(MethodArgumentNotValidException e) {
        String poruka = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getDefaultMessage())
                .orElse("Neispravan zahtjev.");
        return ResponseEntity.badRequest().body(Map.of("poruka", poruka));
    }
}