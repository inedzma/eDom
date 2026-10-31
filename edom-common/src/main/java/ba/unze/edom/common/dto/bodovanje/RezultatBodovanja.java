package ba.unze.edom.common.dto.bodovanje;

import java.util.List;

/**
 * Rezultat bodovanja jedne prijave.
 *
 * @param upozorenja stvari koje administrator mora vidjeti, ali ne blokiraju prijavu
 *                   (npr. student upisuje istu godinu treći put)
 */
public record RezultatBodovanja(
        double potvrdjeno,
        double potencijalno,
        boolean konacno,
        List<StavkaBodovanja> stavke,
        List<String> upozorenja
) {
    /** Za postojeće pozive bez upozorenja. */
    public RezultatBodovanja(double potvrdjeno, double potencijalno,
                             boolean konacno, List<StavkaBodovanja> stavke) {
        this(potvrdjeno, potencijalno, konacno, stavke, List.of());
    }

    /** Ima li nešto što čeka verifikaciju administratora. */
    public boolean cekaVerifikaciju() {
        return potencijalno > potvrdjeno;
    }

    public boolean imaUpozorenja() {
        return upozorenja != null && !upozorenja.isEmpty();
    }
}