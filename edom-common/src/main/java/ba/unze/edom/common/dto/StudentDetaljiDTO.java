package ba.unze.edom.common.dto;

/** Detalji studenta za desktop (klik na ime u sobi). */
public record StudentDetaljiDTO(
        Integer idPrijava,
        String ime,
        String prezime,
        String brojIndeksa,
        String jmbg,
        String email,
        String telefon,
        String fakultet,
        Integer godinaStudija,
        String adresa,
        String imeRoditelja,
        Integer akademskaGodina,
        String statusPrijave,
        double bodovi,
        Integer rang,
        Integer brojSobe,
        String statusSmjestaja,
        String napomena
) {}