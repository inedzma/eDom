package ba.unze.edom.common.dto.uplate;

import java.util.List;

/**
 * Zahtjev desktopa: pošalji podsjetnik o neplaćenim mjesecima ovim studentima.
 *
 * @param napomena dodatni tekst sekretara (nije obavezan)
 */
public record PodsjetnikUplateDTO(int akademskaGodina, List<Integer> idStudenti, String napomena) {}