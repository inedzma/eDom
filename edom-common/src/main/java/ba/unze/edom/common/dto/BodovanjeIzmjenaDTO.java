package ba.unze.edom.common.dto;

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
        boolean uspjehVerifikovan,
        boolean udaljenostVerifikovana,
        boolean primanjaVerifikovana,
        List<BodovanjeKriterijIzmjenaDTO> kriteriji
) {}