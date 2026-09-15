package ba.unze.edom.server.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record DomacinstvoKorakDTO(

        @NotNull(message = "Broj članova domaćinstva je obavezan")
        @Min(value = 1, message = "Domaćinstvo ima najmanje jednog člana")
        @Max(30)
        Integer brojClanovaDomacinstva,

        @NotNull(message = "Ukupna primanja su obavezna")
        @DecimalMin(value = "0.00", message = "Primanja ne mogu biti negativna")
        BigDecimal ukupnaPrimanja,

        @NotNull(message = "Udaljenost je obavezna")
        @DecimalMin(value = "0.00") @DecimalMax(value = "1000.00")
        BigDecimal udaljenostKm
) {}