package ba.unze.edom.common.dto.sobe;

import java.util.List;

/** Sve sobe s trenutnim stanarima za jednu akademsku godinu. */
public record SobePregledDTO(
        int akademskaGodina,
        List<SobaPregledDTO> sobe,
        int ukupnoMjesta,
        int zauzeto,
        int slobodnoM,
        int slobodnoZ,
        int bezSobe
) {
    public int slobodno() {
        return ukupnoMjesta - zauzeto;
    }
}