package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.korisnik.KorisnikIzmjenaDTO;
import ba.unze.edom.common.dto.korisnik.KorisnikProfilDTO;
import ba.unze.edom.common.dto.korisnik.PromjenaLozinkeDTO;
import ba.unze.edom.server.service.korisnik.KorisnikService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Profil trenutno prijavljenog korisnika. Ko je prijavljen, zna se iz tokena. */
@RestController
@RequestMapping("/api/profil")
@RequiredArgsConstructor
public class ProfilApiController {

    private final KorisnikService korisnikService;

    @GetMapping
    public KorisnikProfilDTO profil(Authentication auth) {
        return korisnikService.profil(auth.getName());
    }

    @PutMapping
    public KorisnikProfilDTO izmijeni(Authentication auth,
                                      @Valid @RequestBody KorisnikIzmjenaDTO dto) {
        return korisnikService.izmijeni(auth.getName(), dto);
    }

    @PutMapping("/lozinka")
    public ResponseEntity<Void> promijeniLozinku(Authentication auth,
                                                 @Valid @RequestBody PromjenaLozinkeDTO dto) {
        korisnikService.promijeniLozinku(auth.getName(), dto);
        return ResponseEntity.noContent().build();
    }
}