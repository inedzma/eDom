package ba.unze.edom.common.dto;

import java.time.LocalDate;

public record DokumentDTO(
        Integer idDokument,
        String naziv,
        String vrsta,
        LocalDate datumUpload,
        Boolean dostavljen,
        boolean imaSadrzaj
) {}