package ba.unze.edom.common.dto;

import java.time.LocalDate;

/** Zahtjev za objavu: vrsta = PRELIMINARNA ili KONACNA; termin useljenja ide na web. */
public record ObjavaRangListeDTO(
        String vrsta,
        boolean popuniBrucosima,
        LocalDate useljenjeOd,
        LocalDate useljenjeDo
) {}