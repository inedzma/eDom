package ba.unze.edom.common.util;

import java.util.*;

/**
 * Kantoni (i RS, Brčko) s općinama, za izbor iz liste na webu i desktopu.
 * U bazi se snima tekst. FBiH prema FZS (2026), RS prema spisku UIO BiH + Stanari.
 */
public final class Opcine {

    private Opcine() {}

    public static final Map<String, List<String>> PO_KANTONU;

    static {
        Map<String, List<String>> m = new LinkedHashMap<>();

        m.put("Unsko-sanski kanton", List.of(
                "Bihać", "Bosanska Krupa", "Bosanski Petrovac", "Bužim", "Cazin",
                "Ključ", "Sanski Most", "Velika Kladuša"));

        m.put("Posavski kanton", List.of(
                "Domaljevac-Šamac", "Odžak", "Orašje"));

        m.put("Tuzlanski kanton", List.of(
                "Banovići", "Čelić", "Doboj Istok", "Gračanica", "Gradačac", "Kalesija",
                "Kladanj", "Lukavac", "Sapna", "Srebrenik", "Teočak", "Tuzla", "Živinice"));

        m.put("Zeničko-dobojski kanton", List.of(
                "Breza", "Doboj Jug", "Kakanj", "Maglaj", "Olovo", "Tešanj", "Usora",
                "Vareš", "Visoko", "Zavidovići", "Zenica", "Žepče"));

        m.put("Bosansko-podrinjski kanton Goražde", List.of(
                "Foča-Ustikolina", "Goražde", "Pale-Prača"));

        m.put("Srednjobosanski kanton", List.of(
                "Bugojno", "Busovača", "Dobretići", "Donji Vakuf", "Fojnica",
                "Gornji Vakuf-Uskoplje", "Jajce", "Kiseljak", "Kreševo", "Novi Travnik",
                "Travnik", "Vitez"));

        m.put("Hercegovačko-neretvanski kanton", List.of(
                "Čapljina", "Čitluk", "Jablanica", "Konjic", "Mostar", "Neum",
                "Prozor-Rama", "Ravno", "Stolac"));

        m.put("Zapadnohercegovački kanton", List.of(
                "Grude", "Ljubuški", "Posušje", "Široki Brijeg"));

        m.put("Kanton Sarajevo", List.of(
                "Centar Sarajevo", "Hadžići", "Ilidža", "Ilijaš", "Novi Grad Sarajevo",
                "Novo Sarajevo", "Stari Grad Sarajevo", "Trnovo", "Vogošća"));

        m.put("Kanton 10", List.of(
                "Bosansko Grahovo", "Drvar", "Glamoč", "Kupres", "Livno", "Tomislavgrad"));

        m.put("Republika Srpska", List.of(
                "Banja Luka", "Berkovići", "Bijeljina", "Bileća", "Bratunac", "Brod",
                "Čajniče", "Čelinac", "Derventa", "Doboj", "Donji Žabar", "Foča", "Gacko",
                "Gradiška", "Han Pijesak", "Istočna Ilidža", "Istočni Drvar", "Istočni Mostar",
                "Istočni Stari Grad", "Istočno Novo Sarajevo", "Jezero", "Kalinovik", "Kneževo",
                "Kostajnica", "Kotor Varoš", "Kozarska Dubica", "Krupa na Uni", "Kupres",
                "Laktaši", "Lopare", "Ljubinje", "Milići", "Modriča", "Mrkonjić Grad",
                "Nevesinje", "Novi Grad", "Novo Goražde", "Osmaci", "Oštra Luka", "Pale",
                "Pelagićevo", "Petrovac", "Petrovo", "Prijedor", "Prnjavor", "Ribnik",
                "Rogatica", "Rudo", "Sokolac", "Srbac", "Srebrenica", "Stanari", "Šamac",
                "Šekovići", "Šipovo", "Teslić", "Trebinje", "Trnovo", "Ugljevik", "Višegrad",
                "Vlasenica", "Vukosavlje", "Zvornik"));

        m.put("Brčko distrikt BiH", List.of("Brčko"));

        // strani državljani (konkurs je otvoren i za njih)
        m.put("Izvan BiH", List.of("Izvan BiH"));

        PO_KANTONU = Collections.unmodifiableMap(m);
    }

    public static Set<String> kantoni() {
        return PO_KANTONU.keySet();
    }

    public static List<String> opcine(String kanton) {
        return kanton == null ? List.of() : PO_KANTONU.getOrDefault(kanton, List.of());
    }

    /** Pripada li općina izabranom kantonu (zaštita od ručno izmijenjene forme). */
    public static boolean postoji(String kanton, String opcina) {
        return opcina != null && opcine(kanton).contains(opcina);
    }
}