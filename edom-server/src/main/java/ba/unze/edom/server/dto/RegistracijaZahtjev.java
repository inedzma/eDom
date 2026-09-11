package ba.unze.edom.server.dto;

import jakarta.validation.constraints.*;

public record RegistracijaZahtjev(

        @NotBlank(message = "Ime je obavezno")
        @Size(max = 50)
        String ime,

        @NotBlank(message = "Prezime je obavezno")
        @Size(max = 50)
        String prezime,

        @NotBlank(message = "JMBG je obavezan")
        @Pattern(regexp = "\\d{13}", message = "JMBG mora imati 13 cifara")
        String jmbg,

        @NotBlank(message = "Broj indeksa je obavezan")
        @Size(max = 20)
        String brojIndeksa,

        @NotBlank(message = "E-mail je obavezan")
        @Email(message = "Neispravan format e-mail adrese")
        String email,

        @Size(max = 100)
        String telefon,

        @NotBlank(message = "Fakultet je obavezan")
        String fakultet,

        @NotNull(message = "Godina studija je obavezna")
        @Min(1) @Max(6)
        Integer godinaStudija,

        @NotBlank(message = "Adresa je obavezna")
        @Size(max = 50)
        String adresa,

        @NotBlank(message = "Ime roditelja je obavezno")
        @Size(max = 20)
        String imeRoditelja,

        @NotBlank(message = "Lozinka je obavezna")
        @Size(min = 8, message = "Lozinka mora imati najmanje 8 znakova")
        String lozinka
) {}