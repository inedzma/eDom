package ba.unze.edom.server.dto;

import java.time.LocalDate;

/** Dokument koji je student već priložio — bez Base64 sadržaja. */
public record PrilozeniDokument(
        Integer idDokument,
        String naziv,
        Integer idVrste,
        String nazivVrste,
        LocalDate datumUpload,
        boolean imaSadrzaj
) {
    /** Naziv koji je student upisao (npr. "otac"), ako se razlikuje od vrste. */
    public String opis() {
        return naziv != null && !naziv.equals(nazivVrste) ? naziv : null;
    }
}