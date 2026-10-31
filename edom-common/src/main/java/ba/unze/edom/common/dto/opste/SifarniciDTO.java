package ba.unze.edom.common.dto.opste;

import ba.unze.edom.common.dto.bodovanje.VrstaKriterijaDTO;
import ba.unze.edom.common.dto.dokumenti.VrstaDokumentaDTO;
import ba.unze.edom.common.dto.prijava.SocijalniStatusDTO;

import java.util.List;

public record SifarniciDTO(
        List<SocijalniStatusDTO> socijalniStatusi,
        List<VrstaKriterijaDTO> kriteriji,
        List<VrstaDokumentaDTO> vrsteDokumenata
) {}