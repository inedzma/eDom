package ba.unze.edom.server.service.bodovanje;

import ba.unze.edom.common.dto.opste.PostavkeDTO;
import ba.unze.edom.server.service.prijava.GodineStudija;

import java.util.*;

/** Pravila za uspjeh, udaljenost i primanja, pročitana iz Postavki. */
public record PravilaBodovanja(
        double brucosMnozilac,
        double brucosDodatak,
        double prosjekMnozilac,
        double ispitMnozilac,
        Map<Integer, Double> bonusGodine,
        double kaznaPonovniUpis,
        Skala udaljenost,
        Skala primanja
) {

    // podrazumijevane vrijednosti (Pravilnik 2026/2027)
    public static final String DEF_BRUCOS_MNOZILAC = "9";
    public static final String DEF_BRUCOS_DODATAK = "1";
    public static final String DEF_PROSJEK_MNOZILAC = "3,5";
    public static final String DEF_ISPIT_MNOZILAC = "1,8";
    public static final String DEF_BONUS = "2:3; 3:5; 4:7; 5:9; 6:10; apsolvent:0; postdiplomac:10";
    public static final String DEF_KAZNA = "-3";
    public static final String DEF_UDALJENOST = "<50:0; <80:4; <120:8; <160:12; *:15";
    public static final String DEF_PRIMANJA = "<110:20; <=150:15; <=200:8; <=300:4; <=400:2; *:0";

    /** Jedna granica: "<50:0" ili "<=150:15". */
    public record Prag(boolean ukljucivo, double granica, double bodovi) {
        boolean vrijedi(double x) {
            return ukljucivo ? x <= granica : x < granica;
        }
    }

    /** Niz granica + bodovi za sve ostalo ("*"). */
    public record Skala(List<Prag> pragovi, double ostalo) {
        public double bodovi(double x) {
            for (Prag p : pragovi) if (p.vrijedi(x)) return p.bodovi();
            return ostalo;
        }
    }

    public double bonus(int godina) {
        return bonusGodine.getOrDefault(godina, 0.0);
    }

    // =====================================================================

    /** Prazna polja → podrazumijevano. Neispravan unos → IllegalArgumentException s porukom za admina. */
    public static PravilaBodovanja iz(PostavkeDTO p) {
        return new PravilaBodovanja(
                broj(ili(p.brucosMnozilac(), DEF_BRUCOS_MNOZILAC), "Brucoš – množilac prosjeka"),
                broj(ili(p.brucosDodatak(), DEF_BRUCOS_DODATAK), "Brucoš – dodatak"),
                broj(ili(p.prosjekMnozilac(), DEF_PROSJEK_MNOZILAC), "Množilac prosjeka"),
                broj(ili(p.ispitMnozilac(), DEF_ISPIT_MNOZILAC), "Množilac položenih ispita"),
                bonus(ili(p.bonusGodine(), DEF_BONUS)),
                broj(ili(p.kaznaPonovniUpis(), DEF_KAZNA), "Ponovni upis"),
                skala(ili(p.udaljenostBodovi(), DEF_UDALJENOST), "Udaljenost"),
                skala(ili(p.primanjaBodovi(), DEF_PRIMANJA), "Primanja po članu"));
    }

    private static Skala skala(String tekst, String naziv) {
        List<Prag> pragovi = new ArrayList<>();
        Double ostalo = null;
        for (String dio : tekst.split(";")) {
            dio = dio.trim();
            if (dio.isEmpty()) continue;
            String[] kv = dio.split(":");
            if (kv.length != 2) throw greska(naziv, "dio \"" + dio + "\" nije u obliku <granica:bodovi");
            String lijevo = kv[0].trim();
            double bodovi = broj(kv[1], naziv);
            if (lijevo.equals("*")) {
                ostalo = bodovi;
            } else if (lijevo.startsWith("<=")) {
                pragovi.add(new Prag(true, broj(lijevo.substring(2), naziv), bodovi));
            } else if (lijevo.startsWith("<")) {
                pragovi.add(new Prag(false, broj(lijevo.substring(1), naziv), bodovi));
            } else {
                throw greska(naziv, "granica \"" + lijevo + "\" mora počinjati sa < ili <=");
            }
        }
        if (pragovi.isEmpty()) throw greska(naziv, "upišite bar jednu granicu");
        if (ostalo == null) throw greska(naziv, "dodajte \"*:bodovi\" za sve iznad zadnje granice");
        for (int i = 1; i < pragovi.size(); i++) {
            if (pragovi.get(i).granica() < pragovi.get(i - 1).granica()) {
                throw greska(naziv, "granice moraju ići od manje prema većoj");
            }
        }
        return new Skala(List.copyOf(pragovi), ostalo);
    }

    private static Map<Integer, Double> bonus(String tekst) {
        Map<Integer, Double> m = new HashMap<>();
        for (String dio : tekst.split(";")) {
            dio = dio.trim();
            if (dio.isEmpty()) continue;
            String[] kv = dio.split(":");
            if (kv.length != 2) throw greska("Bonus za godinu", "dio \"" + dio + "\" nije u obliku godina:bodovi");
            String g = kv[0].trim().toLowerCase(Locale.ROOT);
            int godina = switch (g) {
                case "apsolvent" -> GodineStudija.APSOLVENT;
                case "postdiplomac" -> GodineStudija.POSTDIPLOMAC;
                default -> {
                    try {
                        yield Integer.parseInt(g);
                    } catch (NumberFormatException e) {
                        throw greska("Bonus za godinu", "\"" + kv[0].trim() + "\" nije godina");
                    }
                }
            };
            m.put(godina, broj(kv[1], "Bonus za godinu"));
        }
        return Map.copyOf(m);
    }

    private static double broj(String s, String naziv) {
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (RuntimeException e) {
            throw greska(naziv, "\"" + s.trim() + "\" nije broj");
        }
    }

    private static String ili(String s, String def) {
        return s == null || s.isBlank() ? def : s;
    }

    private static IllegalArgumentException greska(String naziv, String opis) {
        return new IllegalArgumentException(naziv + ": " + opis + ".");
    }
}