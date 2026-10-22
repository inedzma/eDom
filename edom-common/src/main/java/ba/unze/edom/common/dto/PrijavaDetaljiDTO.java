package ba.unze.edom.common.dto;

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

        boolean uspjehVerifikovan,
        boolean udaljenostVerifikovana,
        boolean primanjaVerifikovana,
        List<KriterijStanjeDTO> kriteriji,
        String napomenaAdmina,
        boolean mozeProvjeravati,
        List<String> dozvoljeniStatusi,

        // uslov za upis naredne godine (samo viši studenti)
        boolean trebaUslov,        // true = kod odobravanja admin bira ispunjava / ne ispunjava
        boolean imaDokazUslova,
        String uslovUpisa          // tekst za prikaz; null za brucoše
) {

    /** Stari konstruktor — koristi ga PrijaveMapper; uslov dopunjava PrijavaAdminService. */
    public PrijavaDetaljiDTO(
            Integer idPrijava, Integer akademskaGodina, LocalDate datumPrijave, String status, String napomena,
            Integer idStudent, String ime, String prezime, String brojIndeksa, String fakultet,
            String email, String telefon,
            Integer godinaStudija, BigDecimal prosjek, Integer polozeniIspiti, Integer brojClanovaDomacinstva,
            BigDecimal ukupnaPrimanja, BigDecimal udaljenostKm,
            RezultatBodovanja bodovanje, String greska,
            List<DokumentDTO> dokumenti,
            boolean uspjehVerifikovan, boolean udaljenostVerifikovana, boolean primanjaVerifikovana,
            List<KriterijStanjeDTO> kriteriji, String napomenaAdmina, boolean mozeProvjeravati,
            List<String> dozvoljeniStatusi) {
        this(idPrijava, akademskaGodina, datumPrijave, status, napomena,
                idStudent, ime, prezime, brojIndeksa, fakultet, email, telefon,
                godinaStudija, prosjek, polozeniIspiti, brojClanovaDomacinstva, ukupnaPrimanja, udaljenostKm,
                bodovanje, greska, dokumenti,
                uspjehVerifikovan, udaljenostVerifikovana, primanjaVerifikovana,
                kriteriji, napomenaAdmina, mozeProvjeravati, dozvoljeniStatusi,
                false, false, null);
    }

    /** Kopija s podacima o uslovu za upis. */
    public PrijavaDetaljiDTO saUslovom(boolean treba, boolean imaDokaz, String opis) {
        return new PrijavaDetaljiDTO(idPrijava, akademskaGodina, datumPrijave, status, napomena,
                idStudent, ime, prezime, brojIndeksa, fakultet, email, telefon,
                godinaStudija, prosjek, polozeniIspiti, brojClanovaDomacinstva, ukupnaPrimanja, udaljenostKm,
                bodovanje, greska, dokumenti,
                uspjehVerifikovan, udaljenostVerifikovana, primanjaVerifikovana,
                kriteriji, napomenaAdmina, mozeProvjeravati, dozvoljeniStatusi,
                treba, imaDokaz, opis);
    }
}