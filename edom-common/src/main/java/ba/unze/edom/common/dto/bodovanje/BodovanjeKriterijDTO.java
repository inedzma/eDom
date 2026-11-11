package ba.unze.edom.common.dto.bodovanje;

import java.math.BigDecimal;

/** Jedan kriterij iz liste SVIH kriterija (i neoznačenih). */
public record BodovanjeKriterijDTO(
        Integer idVrste,
        String sifra,
        String naziv,
        String grupa,              // BRANIOCI (najveći) ili DODATNI (zbir)
        String pravilo,            // koliko nosi, za prikaz
        boolean traziParametar,
        String nazivParametra,
        boolean izabran,
        BigDecimal parametar,
        double bodovi,
        boolean boduje             // branioci: ova stavka je najveća i ulazi u zbir
) {}