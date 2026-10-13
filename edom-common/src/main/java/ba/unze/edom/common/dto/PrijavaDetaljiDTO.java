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

        List<DokumentDTO> dokumenti
) {}