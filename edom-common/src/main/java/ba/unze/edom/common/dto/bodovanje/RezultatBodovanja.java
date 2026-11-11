package ba.unze.edom.common.dto.bodovanje;

import java.util.List;

/**
 * Rezultat bodovanja jedne prijave.
 *
 * @param upozorenja stvari koje administrator mora vidjeti, ali ne blokiraju prijavu
 *                   (npr. student upisuje istu godinu treći put)
 */
public record RezultatBodovanja(
        double bodovi,
        boolean konacno,
        List<StavkaBodovanja> stavke,
        List<String> upozorenja
) {
    public boolean imaUpozorenja() {
        return upozorenja != null && !upozorenja.isEmpty();
    }
}