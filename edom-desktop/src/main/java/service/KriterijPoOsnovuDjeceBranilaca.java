package service;

import javafx.scene.control.TextField;
import service.BraniociRezultat;

import java.util.Map;

public class KriterijPoOsnovuDjeceBranilaca {

    public BraniociRezultat izracunaj(Map<String, Boolean> stanja,
                                      TextField postotakInvalidnosti,
                                      TextField postInvRoditelja,
                                      TextField brojMjeseci) {

        double maxBodovi = 0;
        String maxNaziv = "";

        // Provjera Student RVI
        if (stanja.getOrDefault("StudentRvi", false)) {
            double postotak = parseDouble(postotakInvalidnosti.getText());
            double bodovi = (postotak < 60) ? 15 : 20;
            if (bodovi > maxBodovi) {
                maxBodovi = bodovi;
                maxNaziv = "Student RVI - Invalidnost " + (int)postotak + "%";
            }
        }

        // Provjera Dijete OSRBiH
        if (stanja.getOrDefault("DijeteOSRBiH", false)) {
            double mjeseci = parseDouble(brojMjeseci.getText());
            double bodovi = mjeseci * 0.3;
            if (bodovi > maxBodovi) {
                maxBodovi = bodovi;
                maxNaziv = "Dijete OSRBiH - " + (int)mjeseci + " mjeseci";
            }
        }

        // Provjera Invalidnost roditelja
        if (stanja.getOrDefault("InvalidnostRoditelja", false)) {
            double postotak = parseDouble(postInvRoditelja.getText());
            double bodovi = postotak * 0.1;
            if (bodovi > maxBodovi) {
                maxBodovi = bodovi;
                maxNaziv = "Invalidnost roditelja - " + (int)postotak + "%";
            }
        }

        // Fiksni bodovi sa nazivima
        Map<String, Double> fiksniBodovi = Map.of(
                "Invalidnost", 10.0,
                "PoginuoBranilac", 50.0,
                "ClanPorodiceSehida", 20.0,
                "DjecaNosilacaRPriznanja", 10.0,
                "StudentLogoras", 10.0,
                "RoditeljLogoras", 5.0,
                "BezJednogRoditelja", 15.0,
                "BezObaRoditelja", 20.0,
                "BezRoditeljskogStaratelja", 20.0,
                "KorisniciSocijalnePomoci", 10.0
        );

        Map<String, String> nazivi = Map.of(
                "Invalidnost", "Invalidnost",
                "PoginuoBranilac", "Dijete poginulog branioca/šehida",
                "ClanPorodiceSehida", "Član porodice šehida",
                "DjecaNosilacaRPriznanja", "Dijete nosioca ratnog priznanja",
                "StudentLogoras", "Student iz logora",
                "RoditeljLogoras", "Roditelj iz logora",
                "BezJednogRoditelja", "Bez jednog roditelja",
                "BezObaRoditelja", "Bez oba roditelja",
                "BezRoditeljskogStaratelja", "Bez roditeljskog staratelja",
                "KorisniciSocijalnePomoci", "Korisnik socijalne pomoći"
        );

        for (String key : fiksniBodovi.keySet()) {
            if (stanja.getOrDefault(key, false)) {
                double bodovi = fiksniBodovi.get(key);
                if (bodovi > maxBodovi) {
                    maxBodovi = bodovi;
                    maxNaziv = nazivi.get(key);
                }
            }
        }

        double zaokruzeno = Math.round(maxBodovi * 100.0) / 100.0;
        return new BraniociRezultat(zaokruzeno, maxNaziv);

    }

    private double parseDouble(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}