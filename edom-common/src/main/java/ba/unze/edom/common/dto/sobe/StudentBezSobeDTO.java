package ba.unze.edom.common.dto.sobe;

/** Odobrena prijava koja još nema sobu. */
public record StudentBezSobeDTO(
        Integer idPrijava,
        String imePrezime,
        String spol,
        Integer godinaStudija,
        int rang,
        double bodovi,
        String zelja,
        String brojIndeksa,
        String email
) {}