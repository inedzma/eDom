package ba.unze.edom.server.validation;

import java.util.ArrayList;
import java.util.List;

public final class LozinkaPravila {

    public static final int MIN = 8;
    public static final int MAX = 72;   // BCrypt granica

    private static final String POSEBNI = "!@#$%^&*()_+-=[]{};':\",.<>/?\\|`~";

    /** Vraca listu neispunjenih uslova. Prazna lista znaci da je lozinka u redu. */
    public static List<String> greske(String lozinka) {

        List<String> g = new ArrayList<>();

        if (lozinka == null || lozinka.isBlank()) {
            g.add("lozinka je obavezna");
            return g;
        }

        if (lozinka.length() < MIN) g.add("najmanje " + MIN + " znakova");
        if (lozinka.length() > MAX) g.add("najviše " + MAX + " znakova");
        if (!lozinka.equals(lozinka.strip())) g.add("bez razmaka na početku i kraju");

        boolean veliko = false, malo = false, cifra = false, poseban = false;

        for (char c : lozinka.toCharArray()) {
            if (Character.isUpperCase(c)) veliko = true;
            else if (Character.isLowerCase(c)) malo = true;
            else if (Character.isDigit(c)) cifra = true;
            else if (POSEBNI.indexOf(c) >= 0) poseban = true;
        }

        if (!veliko)  g.add("jedno veliko slovo");
        if (!malo)    g.add("jedno malo slovo");
        if (!cifra)   g.add("jednu cifru");
        if (!poseban) g.add("jedan poseban znak");

        return g;
    }

    public static boolean ispravna(String lozinka) {
        return greske(lozinka).isEmpty();
    }

    /** Jedna recenica sa svim sto nedostaje. */
    public static String poruka(String lozinka) {
        var g = greske(lozinka);
        return g.isEmpty() ? null : "Lozinka mora sadržavati: " + String.join(", ", g) + ".";
    }

    private LozinkaPravila() {}
}