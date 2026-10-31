package ba.unze.edom.common.dto.sobe;

/** Student u sobi. status: PREDLOZEN / DODIJELJEN / USELJEN */
public record StanarDTO(
        Integer idSmjestaj,
        Integer idPrijava,
        String imePrezime,
        String spol,
        Integer godinaStudija,
        Integer rang,
        String status,
        Integer idOznaka,
        String oznaka,
        String bojaOznake,
        int kljuceviPreuzeti,
        String razlog
) {}