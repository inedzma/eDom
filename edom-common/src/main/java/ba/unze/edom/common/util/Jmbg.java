package ba.unze.edom.common.util;

/** Podaci koji se mogu pročitati iz JMBG-a. */
public final class Jmbg {

    private Jmbg() {}

    /** "M" ili "Z"; null ako JMBG nije ispravnog oblika. Cifre 10–12: 000–499 muški, 500–999 ženski. */
    public static String spol(String jmbg) {
        if (jmbg == null || !jmbg.matches("\\d{13}")) return null;
        return Integer.parseInt(jmbg.substring(9, 12)) < 500 ? "M" : "Z";
    }
}