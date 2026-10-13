package ba.unze.edom.common.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Jedan red u admin listi prijava. */
public record PrijavaListaDTO(
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

        Integer godinaStudija,
        BigDecimal prosjek,

        Double potvrdjeniBodovi,
        Double potencijalniBodovi,
        boolean cekaVerifikaciju,
        int brojDokumenata,

        /** null ako je bodovanje uspjelo, inače poruka validatora */
        String greska
) {}