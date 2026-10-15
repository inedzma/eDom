package ba.unze.edom.common.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record KriterijUnosDTO(
        @NotBlank String sifra,
        BigDecimal parametar,
        boolean verifikovan
) {}