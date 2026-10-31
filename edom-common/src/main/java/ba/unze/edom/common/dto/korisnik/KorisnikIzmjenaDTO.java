package ba.unze.edom.common.dto.korisnik;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KorisnikIzmjenaDTO(
        @NotBlank(message = "Ime je obavezno.") @Size(max = 50) String ime,
        @NotBlank(message = "Prezime je obavezno.") @Size(max = 50) String prezime
) {}