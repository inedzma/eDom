package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.Prijava;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Ko je brucoš i šta je s uslovom za upis — isto pravilo za rang listu i odobravanje. */
public final class UslovUpisa {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private UslovUpisa() {}

    /** Godina iz prijave ima prednost; ako je nema, uzima se iz profila studenta. */
    public static Integer godinaStudija(Prijava p) {
        return p.getGodinaStudija() != null ? p.getGodinaStudija() : p.getStudent().getGodinaStudija();
    }

    /** Brucoš = 1. godina i prvi upis. Stare prijave bez podatka o upisu računaju se kao prvi upis. */
    public static boolean jeBrucos(Prijava p) {
        Integer g = godinaStudija(p);
        Integer put = p.getUpisGodinePut();
        return g != null && g == GodineStudija.PRVA && (put == null || put == 1);
    }

    /** Tekst za admina; null za brucoše (kod njih se uslov ne gleda). */
    public static String opis(Prijava p, String status) {
        if (jeBrucos(p)) return null;

        if (Statusi.NA_CEKANJU.equals(status)) {
            return "Ne ispunjava – lista čekanja (čeka uvjerenje)";
        }
        boolean odobrena = Statusi.ODOBRENO.equals(status) || Statusi.ZAKLJUCENO.equals(status)
                || Statusi.ISELJEN.equals(status);
        if (!odobrena) {
            return "Utvrđuje se pri odobravanju (odobreno = ispunjava, na čekanju = ne ispunjava)";
        }

        LocalDateTime d = p.getDokazUslovaAt();
        if (d == null) return "Ispunjava";
        return p.getDokazUslovaEvidentirao() == null
                ? "Ispunjava (uvjerenje uz prijavu)"
                : "Ispunjava – naknadni dokaz " + d.format(FMT) + " (" + p.getDokazUslovaEvidentirao() + ")";
    }
}