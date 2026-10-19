package ba.unze.edom.server.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record StudijKorakDTO(
        @NotNull(message = "Akademska godina je obavezna.") Integer akademskaGodina,
        @NotNull(message = "Godina studija je obavezna.") @Min(1) @Max(8) Integer godinaStudija,
        @NotNull(message = "Prosjek je obavezan.") BigDecimal prosjek,
        @Min(value = 0, message = "Broj ispita ne može biti negativan.") Integer polozeniIspiti,

        @NotNull(message = "Odaberite koji put upisujete ovu godinu.")
        @Min(1) @Max(4) Integer upisGodinePut,

        @NotBlank(message = "Broj lične karte je obavezan.")
        @Size(max = 20, message = "Broj lične karte je predug.")
        @Pattern(regexp = "^$|[A-Za-z0-9]+", message = "Broj lične karte smije imati samo slova i brojeve.")
        String brojLicneKarte,

        @NotBlank(message = "Telefon roditelja je obavezan.")
        @Pattern(regexp = "^$|[0-9+()\\-\\s]{6,30}", message = "Neispravan broj telefona.")
        String telefonRoditelja,

        @NotBlank(message = "Odaberite kanton.") String kanton,
        @NotBlank(message = "Odaberite općinu.") String opcina
) {}