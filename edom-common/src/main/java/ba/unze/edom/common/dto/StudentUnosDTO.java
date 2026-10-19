package ba.unze.edom.common.dto;

import jakarta.validation.constraints.*;

/** Podaci studenta — isti za ručni unos i registraciju na webu. */
public record StudentUnosDTO(
        @NotBlank(message = "Ime je obavezno.") @Size(max = 50) String ime,
        @NotBlank(message = "Prezime je obavezno.") @Size(max = 50) String prezime,
        @NotBlank(message = "JMBG je obavezan.")
        @Pattern(regexp = "\\d{13}", message = "JMBG mora imati 13 cifara.") String jmbg,
        @NotBlank(message = "Broj indeksa je obavezan.") @Size(max = 20) String brojIndeksa,
        @NotBlank(message = "Email je obavezan.")
        @Email(message = "Neispravan format email adrese.") String email,
        @Size(max = 100) String telefon,
        @NotBlank(message = "Fakultet je obavezan.") String fakultet,
        @NotNull(message = "Godina studija je obavezna.") @Min(1) @Max(8) Integer godinaStudija,
        @NotBlank(message = "Adresa je obavezna.") @Size(max = 50) String adresa,
        @NotBlank(message = "Ime roditelja je obavezno.") @Size(max = 20) String imeRoditelja,
        Integer idSocijalnogStatusa,

        // traži se u prijavi (web korak 1, desktop ručni unos), ne pri registraciji
        @Size(max = 20) String brojLicneKarte,
        @Size(max = 30) String telefonRoditelja,
        @Size(max = 60) String kanton,
        @Size(max = 60) String opcina
) {}