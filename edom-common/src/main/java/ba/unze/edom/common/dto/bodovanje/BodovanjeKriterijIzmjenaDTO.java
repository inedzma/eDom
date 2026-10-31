package ba.unze.edom.common.dto.bodovanje;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Označen kriterij koji admin šalje pri snimanju. Neoznačeni se ne šalju (brišu se). */
public record BodovanjeKriterijIzmjenaDTO(
        Integer idVrste,
        BigDecimal parametar,
        LocalDate datumOd,
        LocalDate datumDo,
        boolean verifikovan
) {}