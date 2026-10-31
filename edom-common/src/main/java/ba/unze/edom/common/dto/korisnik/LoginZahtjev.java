package ba.unze.edom.common.dto.korisnik;

import jakarta.validation.constraints.NotBlank;

public record LoginZahtjev(@NotBlank String username, @NotBlank String lozinka) {}