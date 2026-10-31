package ba.unze.edom.server.api;

import ba.unze.edom.server.service.korisnik.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import ba.unze.edom.common.dto.korisnik.LoginOdgovor;
import ba.unze.edom.common.dto.korisnik.LoginZahtjev;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginOdgovor> login(@Valid @RequestBody LoginZahtjev zahtjev) {
        String token = authService.prijaviAdmina(zahtjev.username(), zahtjev.lozinka());
        return ResponseEntity.ok(new LoginOdgovor(token));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> neispravno(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("poruka", e.getMessage()));
    }
}