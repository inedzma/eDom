package ba.unze.edom.common.dto;

import java.math.BigDecimal;

public record KriterijStanjeDTO(
        Integer idPrijavaKriterij,
        String naziv,
        BigDecimal parametar,
        String nazivParametra,
        boolean verifikovan
) {}