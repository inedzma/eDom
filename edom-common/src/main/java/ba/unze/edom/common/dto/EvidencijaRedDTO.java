package ba.unze.edom.common.dto;

import java.time.LocalDate;

/** Jedan red centralne evidencije: prijava + student + smještaj. */
public record EvidencijaRedDTO(
        Integer idPrijava,
        Integer idStudent,
        String sifra,

        String ime,
        String prezime,
        String imeRoditelja,
        String jmbg,
        String brojLicneKarte,
        String brojIndeksa,
        String email,
        String telefon,
        String telefonRoditelja,
        String spol,
        String kanton,
        String opcina,
        String adresa,

        String fakultet,
        Integer godinaStudija,
        Integer akademskaGodina,
        LocalDate datumPrijave,
        String status,
        String kategorija,          // Brucoš / S uslovom / Lista čekanja / —
        boolean primljen,           // iznad crte na rang listi
        Double bodovi,

        Integer soba,
        String statusSmjestaja,     // PREDLOZEN / DODIJELJEN / USELJEN / ISELJEN / null
        LocalDate datumUseljenja,
        LocalDate datumIseljenja,
        int kljuceviPreuzeti,
        int kljuceviVraceni,

        LocalDate karticaPreuzeta,
        LocalDate karticaRazduzena,
        LocalDate ljekarskoPredano,
        LocalDate uvjerenjeFakultetPredano,
        boolean uvjerenjeUzPrijavu,

        String napomena,

        String napomenaPrijave      // napomena uz prijavu (piše student, admin može ispraviti)
) {}