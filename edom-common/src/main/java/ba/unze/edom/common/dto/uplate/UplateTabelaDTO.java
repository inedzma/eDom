package ba.unze.edom.common.dto.uplate;

import java.time.LocalDate;
import java.util.List;

/**
 * Tabela uplata za jednu akademsku godinu: red po studentu, kolona po mjesecu (X–VII).
 *
 * @param mjeseci mjeseci godine u obliku "2026-10", "2026-11", … "2027-07"
 */
public record UplateTabelaDTO(int akademskaGodina, List<String> mjeseci, List<Red> redovi) {

    /** Mjesec je plaćen (datum = kad je plaćeno). */
    public static final String PLACENO = "PLACENO";
    /** Student je bio u domu, mjesec je prošao, a uplate nema. */
    public static final String DUG = "DUG";
    /** Student je u domu, ali mjesec je tekući ili tek dolazi. */
    public static final String CEKA = "CEKA";
    /** Student tada nije bio u domu (prije useljenja ili poslije iseljenja). */
    public static final String NIJE_U_DOMU = "NIJE_U_DOMU";

    /** @param dug broj prošlih mjeseci u domu bez uplate */
    public record Red(int idStudent, String student, Integer soba, List<Celija> celije, int dug) {}

    /** @param datum datum uplate, samo kad je stanje PLACENO */
    public record Celija(String mjesec, String stanje, LocalDate datum) {}
}