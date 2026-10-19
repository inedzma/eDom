package ba.unze.edom.common.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record KriterijUnosDTO(
        @NotBlank String sifra,
        BigDecimal parametar,
        boolean verifikovan,
        LocalDate datumOd,
        LocalDate datumDo
) {
    /** Kriterij bez perioda. */
    public KriterijUnosDTO(String sifra, BigDecimal parametar, boolean verifikovan) {
        this(sifra, parametar, verifikovan, null, null);
    }
}