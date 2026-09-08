package util;

import java.util.List;
import java.util.Map;
import dao.DokumentDAO;
import dao.StudentDAO;
import java.util.HashMap;
import model.Dokument;
import model.Prijava;
import model.Student;

public class BodoviUtil {

    public static Map<String, Double> izracunajBodoveZaPrijavu(Prijava p) {
        // 1. Dohvatanje podataka
        List<Dokument> dokumenti = new DokumentDAO().dohvatiDokumenteZaPrijavu(p.getIdPrijava());
        Student student = new StudentDAO().dohvatiStudentaPoId(p.getIdStudent());

        // 2. Fiksni bodovi za godinu studija
        double godinaBodovi = switch (student.getGodinaStudija()) {
            case 1 -> 1; // Brucoš
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 7;
            case 5 -> 9;
            case 6, 8 -> 10;
            case 7 -> 12;
            default -> 0;
        };

        // 3. Tražimo dokumente sa tvoje slike (ID 7, 8, 9)
        double ukupnoBodoviSaDokumentaUspjeh = 0;

        for (Dokument d : dokumenti) {
            // Ako je dokument null ili vrsta null, preskoci
            if(d.getVrstaDokumenta() == null) continue;

            String naziv = d.getVrstaDokumenta().getNaziv().toLowerCase();
            double bodovi = d.getBrojBodova();

            // --- OVDJE JE BILA GREŠKA ---
            // Prilagođeno tvojoj slici baze (bez kvačica)
            if (naziv.contains("svjedo") ||   // Hvata "Svjedodzba" (ID 7)
                    naziv.contains("indeks") ||   // Hvata "Ovjerena kopija indeksa" (ID 9)
                    naziv.contains("uvjerenje") || // Hvata "Uvjerenje o polozenim" (ID 8)
                    naziv.contains("ocjena") ||    // Dodatna sigurnost
                    naziv.contains("prosjek")) {

                ukupnoBodoviSaDokumentaUspjeh += bodovi;
            }
        }

        // 4. Računanje čistih bodova za kolonu "Prosjek"
        double bodoviUspjeh;

        if (ukupnoBodoviSaDokumentaUspjeh <= 0) {
            // Nema unesenog dokumenta ili su bodovi 0 -> Prosjek je 0
            bodoviUspjeh = 0;
        } else {
            // Imamo bodove (npr. 25). Oduzimamo godinu (npr. 3) da dobijemo samo bodove prosjeka (22)
            bodoviUspjeh = ukupnoBodoviSaDokumentaUspjeh - godinaBodovi;

            // Sigurnosna zaštita od minusa
            if (bodoviUspjeh < 0) bodoviUspjeh = 0;
        }

        // 5. Ostale kategorije (Nagrade, Socijalni, itd.)
        // Koristimo .contains za djelimično podudaranje jer je sigurnije
        double nagradeBodovi = dokumenti.stream()
                .filter(d -> d.getVrstaDokumenta() != null && d.getVrstaDokumenta().getNaziv().toLowerCase().contains("nagrad")) // ID 10
                .mapToDouble(Dokument::getBrojBodova).sum();

        double socijalniBodovi = dokumenti.stream()
                .filter(d -> d.getVrstaDokumenta() != null &&
                        (d.getVrstaDokumenta().getNaziv().toLowerCase().contains("primanja") || // ID 5
                                d.getVrstaDokumenta().getNaziv().toLowerCase().contains("birou") ||    // ID 11
                                d.getVrstaDokumenta().getNaziv().toLowerCase().contains("nezaposlen") || // ID 12
                                d.getVrstaDokumenta().getNaziv().toLowerCase().contains("domać")))
                .mapToDouble(Dokument::getBrojBodova).sum();

        double udaljenostBodovi = dokumenti.stream()
                .filter(d -> d.getVrstaDokumenta() != null && d.getVrstaDokumenta().getNaziv().toLowerCase().contains("cips")) // ID 6
                .mapToDouble(Dokument::getBrojBodova).sum();

        double dodatniBodovi = dokumenti.stream()
                .filter(d -> d.getVrstaDokumenta() != null &&
                        (d.getVrstaDokumenta().getNaziv().toLowerCase().contains("invalid") ||
                                d.getVrstaDokumenta().getNaziv().toLowerCase().contains("izbjeg") ||
                                d.getVrstaDokumenta().getNaziv().toLowerCase().contains("logor") ||
                                d.getVrstaDokumenta().getNaziv().toLowerCase().contains("poginul") ||
                                d.getVrstaDokumenta().getNaziv().toLowerCase().contains("branitelj")))
                .mapToDouble(Dokument::getBrojBodova).sum();

        // 6. Pakovanje u mapu
        Map<String, Double> bodoviMap = new HashMap<>();
        bodoviMap.put("uspjeh", bodoviUspjeh);
        bodoviMap.put("godina", godinaBodovi);
        bodoviMap.put("nagrade", nagradeBodovi);
        bodoviMap.put("socijalni", socijalniBodovi);
        bodoviMap.put("udaljenost", udaljenostBodovi);
        bodoviMap.put("dodatni", dodatniBodovi);
        bodoviMap.put("ukupno", p.getUkupniBodovi()); // Ovo uzimaš direktno iz tabele prijava

        return bodoviMap;
    }
}