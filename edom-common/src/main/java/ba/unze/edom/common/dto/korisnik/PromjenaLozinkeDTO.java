package ba.unze.edom.common.dto.korisnik;

import jakarta.validation.constraints.NotBlank;

public record PromjenaLozinkeDTO(
        @NotBlank(message = "Unesite trenutnu lozinku.") String staraLozinka,
        @NotBlank(message = "Unesite novu lozinku.") String novaLozinka
) {}