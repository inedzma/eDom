package ba.unze.edom.common.util;

/** Ime i prezime u ispravnom obliku: "amra" / "AMRA" → "Amra", "ana-marija" → "Ana-Marija". */
public final class Imena {

    private Imena() {}

    public static String formatiraj(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        StringBuilder sb = new StringBuilder(t.length());
        boolean pocetak = true;
        for (char c : t.toCharArray()) {
            sb.append(pocetak ? Character.toUpperCase(c) : Character.toLowerCase(c));
            pocetak = c == ' ' || c == '-' || c == '\'';
        }
        return sb.toString();
    }
}