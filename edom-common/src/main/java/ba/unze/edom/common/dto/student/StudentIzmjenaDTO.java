package ba.unze.edom.common.dto.student;

import jakarta.validation.constraints.*;

/** Izmjena podataka studenta (administrator na desktopu). */
public record StudentIzmjenaDTO(

        @NotBlank(message = "Ime je obavezno.")
        @Size(max = 50, message = "Ime je predugo.")
        String ime,

        @NotBlank(message = "Prezime je obavezno.")
        @Size(max = 50, message = "Prezime je predugo.")
        String prezime,

        @Size(max = 20, message = "Ime roditelja je predugo.")
        String imeRoditelja,

        @NotBlank(message = "JMBG je obavezan.")
        @Pattern(regexp = "\\d{13}", message = "JMBG mora imati 13 cifara.")
        String jmbg,

        @Size(max = 20, message = "Broj indeksa je predug.")
        String brojIndeksa,

        @NotBlank(message = "Fakultet je obavezan.")
        String fakultet,

        @Email(message = "Neispravan format e-mail adrese.")
        @Size(max = 50, message = "E-mail je predug.")
        String email,

        @Size(max = 100, message = "Telefon je predug.")
        String telefon,

        @Size(max = 50, message = "Adresa je preduga.")
        String adresa,

        @Size(max = 20, message = "Broj lične karte je predug.")
        String brojLicneKarte,

        @Size(max = 30, message = "Telefon roditelja je predug.")
        String telefonRoditelja,

        String kanton,
        String opcina
) {}