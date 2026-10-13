package ba.unze.edom.server.api;

import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import ba.unze.edom.server.exception.PrijavaException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "ba.unze.edom.server.api")
public class ApiGreskeHandler {

    @ExceptionHandler({PrijavaException.class, NevalidnaPrijavaException.class})
    public ResponseEntity<Map<String, String>> poslovnaGreska(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("poruka", e.getMessage()));
    }
}