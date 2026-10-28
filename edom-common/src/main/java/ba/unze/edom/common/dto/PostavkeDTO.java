package ba.unze.edom.common.dto;

import java.time.LocalDate;

/** Postavke konkursa, Studentskog centra i pravila bodovanja (ekran "Postavke" na desktopu). */
public record PostavkeDTO(
        // ---- konkurs ----
        Integer akademskaGodina,        // 2026 = 2026/2027
        LocalDate konkursObjavljen,
        LocalDate prijaveOd,
        LocalDate prijaveDo,            // krajnji rok; null = bez ograničenja
        Integer kapacitet,
        Integer udioBrucosaPosto,       // 35
        Integer minUdaljenostKm,        // 20

        // ---- rang lista i useljenje ----
        Integer rokZalbeDana,
        String useljenjeRadnoVrijeme,   // "8:30 – 14:00"
        String uplataPriUseljenju,      // "150,00"

        // ---- uplata za obradu prijave ----
        String iznosObrade,             // "10,00"
        String racunUplate,
        String budzetskaOrganizacija,
        String pozivNaBroj,
        String vrstaPrihoda,

        // ---- Studentski centar ----
        String direktor,
        String direktorFunkcija,        // "DIREKTORICA"
        String adresa,
        String telefon,
        String email,
        String brojOdlukeKonkurs,
        String brojOdlukeKriteriji,
        String brojRangListe,           // "09-130-020"

        // ---- ugovor ----
        String cijenaSmjestaja,         // "165,00" mjesečno
        String ugovorPeriodOd,          // "01.10."
        String ugovorPeriodDo,          // "15.07."

        // ---- pravila bodovanja ----
        String brucosMnozilac,          // "9"    → prosjek × 9 + 1
        String brucosDodatak,           // "1"
        String prosjekMnozilac,         // "3,5"  → prosjek × 3,5 + ispiti × 1,8 + bonus
        String ispitMnozilac,           // "1,8"
        String bonusGodine,             // "2:3; 3:5; ...; apsolvent:0; postdiplomac:10"
        String kaznaPonovniUpis,        // "-3"
        String udaljenostBodovi,        // "<50:0; <80:4; <120:8; <160:12; *:15"
        String primanjaBodovi,          // "<110:20; <=150:15; <=200:8; <=300:4; <=400:2; *:0"

        // ---- samo za prikaz ----
        String izmijenio,
        String izmijenjeno
) {}