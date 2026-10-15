package ba.unze.edom.common.dto;

import java.util.List;

public record SifarniciDTO(
        List<SocijalniStatusDTO> socijalniStatusi,
        List<VrstaKriterijaDTO> kriteriji,
        List<VrstaDokumentaDTO> vrsteDokumenata
) {}