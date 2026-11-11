package ba.unze.edom.common.dto.bodovanje;

import java.math.BigDecimal;
import java.util.List;

public record BodovanjeIzmjenaDTO(
        Integer godinaStudija,
        Integer upisGodinePut,
        BigDecimal prosjek,
        Integer polozeniIspiti,
        BigDecimal udaljenostKm,
        Integer brojClanova,
        BigDecimal ukupnaPrimanja,
        List<BodovanjeKriterijIzmjenaDTO> kriteriji
) {}