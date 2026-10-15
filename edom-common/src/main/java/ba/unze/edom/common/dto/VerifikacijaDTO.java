package ba.unze.edom.common.dto;

import java.util.List;

public record VerifikacijaDTO(
        boolean uspjeh,
        boolean udaljenost,
        boolean primanja,
        List<KriterijVerifikacijaDTO> kriteriji
) {}