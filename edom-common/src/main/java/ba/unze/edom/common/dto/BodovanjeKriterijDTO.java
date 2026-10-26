package ba.unze.edom.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Jedan kriterij iz liste SVIH kriterija (i neoznačenih). */
public record BodovanjeKriterijDTO(
        Integer idVrste,
        String sifra,
        String naziv,
        String grupa,              // BRANIOCI (najveći) ili DODATNI (zbir)
        String pravilo,            // koliko nosi, za prikaz
        boolean traziParametar,
        String nazivParametra,
        boolean traziPeriod,
        boolean izabran,
        BigDecimal parametar,
        LocalDate datumOd,
        LocalDate datumDo,
        boolean verifikovan,
        double bodovi,
        boolean boduje             // branioci: ova stavka je najveća i ulazi u zbir
) {}