package ba.unze.edom.common.dto.sobe;

import java.util.List;

/** Rezultat "Napravi prijedlog". Prijedlog je već upisan kao PREDLOZEN smještaj. */
public record PrijedlogRasporedaDTO(
        List<Stavka> stavke,
        int rasporedjeno,
        int bezMjesta
) {
    /** brojSobe null = nema slobodnog mjesta */
    public record Stavka(
            Integer idPrijava,
            int rang,
            String imePrezime,
            String spol,
            Integer godinaStudija,
            Integer brojSobe,
            int kapacitetSobe,
            String razlog,
            boolean zeljaIspunjena
    ) {}
}