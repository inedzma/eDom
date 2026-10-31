package ba.unze.edom.common.dto.korisnik;

import java.time.Instant;

public record KorisnikProfilDTO(
        String ime,
        String prezime,
        String username,
        String email,
        String uloga,
        Instant zadnjaPrijava
) {}