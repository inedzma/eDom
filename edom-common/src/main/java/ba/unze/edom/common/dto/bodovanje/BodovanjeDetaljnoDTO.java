package ba.unze.edom.common.dto.bodovanje;

import java.math.BigDecimal;
import java.util.List;

/** Sve za tabelu bodovanja u detaljima prijave. */
public record BodovanjeDetaljnoDTO(
        Integer idPrijava,

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

        List<StavkaBodovanja> osnovne,          // uspjeh, ponovni upis, udaljenost, primanja
        List<BodovanjeKriterijDTO> kriteriji,   // SVI aktivni kriteriji

        double potvrdjeno,
        double potencijalno,
        List<String> upozorenja,
        String greska
) {}