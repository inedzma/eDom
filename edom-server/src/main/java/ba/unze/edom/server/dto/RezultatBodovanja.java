package ba.unze.edom.server.dto;

import java.util.List;

public record RezultatBodovanja(
        double potvrdjeno,
        double potencijalno,
        List<StavkaBodovanja> stavke
) {
    /** Ima li nesto sto ceka verifikaciju administratora. */
    public boolean cekaVerifikaciju() {
        return potencijalno > potvrdjeno;
    }
}