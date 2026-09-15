package ba.unze.edom.server.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record StudijKorakDTO(

        @NotNull(message = "Akademska godina je obavezna")
        @Min(2020) @Max(2100)
        Integer akademskaGodina,

        @NotNull(message = "Godina studija je obavezna")
        @Min(1) @Max(8)
        Integer godinaStudija,

        @NotNull(message = "Prosjek je obavezan")
        @DecimalMin(value = "1.00") @DecimalMax(value = "10.00")
        BigDecimal prosjek,

        @Min(value = 0, message = "Broj ispita ne može biti negativan")
        @Max(100)
        Integer polozeniIspiti
) {}