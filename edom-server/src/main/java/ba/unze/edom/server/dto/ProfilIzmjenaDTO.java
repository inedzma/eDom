package ba.unze.edom.server.dto;

import jakarta.validation.constraints.*;

public record ProfilIzmjenaDTO(

        @NotBlank(message = "E-mail je obavezan")
        @Email(message = "Neispravan format e-mail adrese")
        @Size(max = 50)
        String email,

        @Size(max = 100)
        String telefon,

        @NotBlank(message = "Adresa je obavezna")
        @Size(max = 50)
        String adresa,

        @NotBlank(message = "Fakultet je obavezan")
        @Size(max = 100)
        String fakultet,

        @NotNull(message = "Godina studija je obavezna")
        @Min(1) @Max(6)
        Integer godinaStudija
) {}