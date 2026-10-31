package ba.unze.edom.common.dto.prijava;

import java.util.List;

public record VerifikacijaDTO(
        boolean uspjeh,
        boolean udaljenost,
        boolean primanja,
        List<KriterijVerifikacijaDTO> kriteriji
) {}