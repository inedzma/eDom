package ba.unze.edom.common.util;

import java.util.List;

/** Jedina lista fakulteta: desktop, web registracija i server provjera. */
public final class Fakulteti {

    private Fakulteti() {}

    public static final List<String> SVI = List.of(
            "Politehnički fakultet UNZE",
            "Pravni fakultet UNZE",
            "Ekonomski fakultet UNZE",
            "Filozofski fakultet UNZE",
            "Medicinski fakultet UNZE",
            "Mašinski fakultet UNZE",
            "Islamsko pedagoški fakultet UNZE",
            "Fakultet inženjerstva i prirodnih nauka UNZE"
    );

    public static boolean postoji(String naziv) {
        return naziv != null && SVI.contains(naziv);
    }
}