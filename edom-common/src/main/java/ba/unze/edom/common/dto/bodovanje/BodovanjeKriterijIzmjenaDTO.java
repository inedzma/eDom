package ba.unze.edom.common.dto.bodovanje;

import java.math.BigDecimal;

/** Označen kriterij koji admin šalje pri snimanju. Neoznačeni se ne šalju (brišu se). */
public record BodovanjeKriterijIzmjenaDTO(
        Integer idVrste,
        BigDecimal parametar
) {}