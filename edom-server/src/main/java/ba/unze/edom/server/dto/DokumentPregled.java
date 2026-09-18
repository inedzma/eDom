package ba.unze.edom.server.dto;

import java.time.LocalDate;

/**
 * Projekcija za prikaz liste dokumenata.
 * Namjerno NE sadrzi dokumentB64 - taj se cita samo pri preuzimanju.
 */
public interface DokumentPregled {

    Integer getIdDokument();
    String getNaziv();
    LocalDate getDatumUpload();
    Boolean getDostavljen();
    VrstaPregled getVrstaDokumenta();

    interface VrstaPregled {
        Integer getIdVrsta();
        String getNaziv();
    }
}