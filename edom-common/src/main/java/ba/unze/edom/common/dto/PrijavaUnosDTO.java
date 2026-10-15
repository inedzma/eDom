package ba.unze.edom.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PrijavaUnosDTO(
        @NotNull(message = "Akademska godina je obavezna.") Integer akademskaGodina,
        @NotNull(message = "Prosjek je obavezan.") BigDecimal prosjek,
        Integer polozeniIspiti,
        @NotNull(message = "Broj članova domaćinstva je obavezan.")
        @Min(value = 1, message = "Domaćinstvo mora imati najmanje jednog člana.") Integer brojClanovaDomacinstva,
        BigDecimal ukupnaPrimanja,
        @NotNull(message = "Udaljenost je obavezna.") BigDecimal udaljenostKm,
        String napomena,

        /** original pregledan pri ručnom unosu */
        boolean uspjehVerifikovan,
        boolean udaljenostVerifikovana,
        boolean primanjaVerifikovana
) {}