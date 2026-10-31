package ba.unze.edom.common.dto.dokumenti;

import java.time.LocalDate;

public record DokumentDTO(
        Integer idDokument,
        String naziv,
        String vrsta,
        LocalDate datumUpload,
        Boolean dostavljen,
        boolean imaSadrzaj
) {}