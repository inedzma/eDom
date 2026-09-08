package service;

import java.util.List;
import model.Dokument;

public class KriterijPoOsnovuSocijalnogStatusa {

    // Bodovi za primanja (GLAVNI KRITERIJ)
    public static double bodoviZaPrimanja(double iznosPoClanu) {
        if (iznosPoClanu < 110) return 20;
        if (iznosPoClanu <= 150) return 15;
        if (iznosPoClanu <= 200) return 8;
        if (iznosPoClanu <= 300) return 4;
        if (iznosPoClanu <= 400) return 2;
        return 0; // > 400
    }

    // Bodovi po dokumentima (koristit ćeš tek kad dodaš UI za to)
    public static double bodoviZaDokumente(List<Dokument> dokumenti) {
        double suma = 0;

        for (Dokument d : dokumenti) {
            if (!d.isDostavljen()) continue;

            switch (d.getVrstaDokumenta().getNaziv()) {
                case "Izbjeglica":
                    suma += 3;
                    break;
                case "Brat ili sestra student":
                    suma += 2;
                    break;
                case "Bez oba roditelja":
                    suma += 10;
                    break;
            }
        }

        return suma;
    }

    // UKUPNO BODOVANJE PRIJAVE
    public static double izracunajUkupno(double primanjaPoClanu, List<Dokument> dokumenti) {
        return bodoviZaPrimanja(primanjaPoClanu) + bodoviZaDokumente(dokumenti);
    }
}
