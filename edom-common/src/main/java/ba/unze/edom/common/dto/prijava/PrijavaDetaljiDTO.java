package ba.unze.edom.common.dto.prijava;

import ba.unze.edom.common.dto.bodovanje.RezultatBodovanja;
import ba.unze.edom.common.dto.dokumenti.DokumentDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PrijavaDetaljiDTO(
        Integer idPrijava,
        Integer akademskaGodina,
        LocalDate datumPrijave,
        String status,
        String napomena,

        Integer idStudent,
        String ime,
        String prezime,
        String brojIndeksa,
        String fakultet,
        String email,
        String telefon,

        Integer godinaStudija,
        BigDecimal prosjek,
        Integer polozeniIspiti,
        Integer brojClanovaDomacinstva,
        BigDecimal ukupnaPrimanja,
        BigDecimal udaljenostKm,

        RezultatBodovanja bodovanje,   // null ako validator odbije podatke
        String greska,

        List<DokumentDTO> dokumenti,
        String napomenaAdmina,
        List<String> dozvoljeniStatusi,

        // uslov za upis naredne godine (samo viši studenti)
        boolean trebaUslov,        // true = kod odobravanja admin bira ispunjava / ne ispunjava
        boolean imaDokazUslova,
        String uslovUpisa          // tekst za prikaz; null za brucoše
) {

    /** Bez podataka o uslovu — koristi ga PrijaveMapper; uslov dopunjava PrijavaAdminService. */
    public PrijavaDetaljiDTO(
            Integer idPrijava, Integer akademskaGodina, LocalDate datumPrijave, String status, String napomena,
            Integer idStudent, String ime, String prezime, String brojIndeksa, String fakultet,
            String email, String telefon,
            Integer godinaStudija, BigDecimal prosjek, Integer polozeniIspiti, Integer brojClanovaDomacinstva,
            BigDecimal ukupnaPrimanja, BigDecimal udaljenostKm,
            RezultatBodovanja bodovanje, String greska,
            List<DokumentDTO> dokumenti, String napomenaAdmina, List<String> dozvoljeniStatusi) {
        this(idPrijava, akademskaGodina, datumPrijave, status, napomena,
                idStudent, ime, prezime, brojIndeksa, fakultet, email, telefon,
                godinaStudija, prosjek, polozeniIspiti, brojClanovaDomacinstva, ukupnaPrimanja, udaljenostKm,
                bodovanje, greska, dokumenti, napomenaAdmina, dozvoljeniStatusi,
                false, false, null);
    }

    /** Kopija s podacima o uslovu za upis. */
    public PrijavaDetaljiDTO saUslovom(boolean treba, boolean imaDokaz, String opis) {
        return new PrijavaDetaljiDTO(idPrijava, akademskaGodina, datumPrijave, status, napomena,
                idStudent, ime, prezime, brojIndeksa, fakultet, email, telefon,
                godinaStudija, prosjek, polozeniIspiti, brojClanovaDomacinstva, ukupnaPrimanja, udaljenostKm,
                bodovanje, greska, dokumenti, napomenaAdmina, dozvoljeniStatusi,
                treba, imaDokaz, opis);
    }
}