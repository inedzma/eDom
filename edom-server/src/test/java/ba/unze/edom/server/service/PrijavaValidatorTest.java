package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.entity.VrstaKriterija;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import ba.unze.edom.server.service.prijava.PrijavaValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Provjera da su podaci u prijavi smisleni prije bodovanja.
 * Za svako pravilo testira se zadnja vrijednost koja prolazi i prva koja ne prolazi.
 */
class PrijavaValidatorTest {

    private final PrijavaValidator validator = new PrijavaValidator();

    private Prijava prijava() {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(4);
        return p;
    }

    private void prolazi(Prijava p) {
        assertDoesNotThrow(() -> validator.provjeri(p));
    }

    private String pada(Prijava p) {
        return assertThrows(NevalidnaPrijavaException.class, () -> validator.provjeri(p)).getMessage();
    }

    // =====================================================================
    // OPŠTE
    // =====================================================================

    @Test
    @DisplayName("Prazna prijava prolazi (student je popunjava korak po korak)")
    void praznaProlazi() {
        prolazi(new Prijava());
    }

    @Test
    @DisplayName("null prijava prolazi")
    void nullProlazi() {
        assertDoesNotThrow(() -> validator.provjeri(null));
    }

    @Test
    @DisplayName("Potpuno i ispravno popunjena prijava prolazi")
    void kompletnaIspravna() {
        Prijava p = prijava();
        p.setGodinaStudija(3);
        p.setUpisGodinePut(1);
        p.setProsjek(new BigDecimal("8.50"));
        p.setPolozeniIspiti(20);
        p.setUdaljenostKm(new BigDecimal("95"));
        p.setUkupnaPrimanja(new BigDecimal("1200"));
        dodaj(p, kriterij("STUDENT_RVI"), 60.0);
        dodaj(p, kriterij("DIJETE_OSRBIH"), 44.0);
        dodaj(p, fiksni("IZBJEGLICA"), null);
        prolazi(p);
    }

    // =====================================================================
    // GODINA STUDIJA I PONOVNI UPIS
    // =====================================================================

    @Nested
    @DisplayName("Godina studija i ponovni upis")
    class Godina {

        @ParameterizedTest(name = "godina {0} se odbija")
        @ValueSource(ints = {-1, 0, 9, 20})
        @DisplayName("Godina izvan 1–8 se odbija")
        void nevalidna(int g) {
            Prijava p = prijava();
            p.setGodinaStudija(g);
            assertTrue(pada(p).contains("Godina studija"));
        }

        @ParameterizedTest(name = "godina {0} prolazi")
        @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
        @DisplayName("Godine 1–8 (uključujući apsolventa i postdiplomca) prolaze")
        void validna(int g) {
            Prijava p = prijava();
            p.setGodinaStudija(g);
            prolazi(p);
        }

        @ParameterizedTest(name = "{0}. put prolazi")
        @ValueSource(ints = {1, 2, 3, 4})
        @DisplayName("Upis iste godine 1–4 puta prolazi (kao na web formi)")
        void upisPutValidan(int put) {
            Prijava p = prijava();
            p.setUpisGodinePut(put);
            prolazi(p);
        }

        @ParameterizedTest(name = "{0}. put se odbija")
        @ValueSource(ints = {-1, 0, 5})
        @DisplayName("Upis iste godine 0 ili više od 4 puta se odbija")
        void upisPutNevalidan(int put) {
            Prijava p = prijava();
            p.setUpisGodinePut(put);
            assertTrue(pada(p).contains("Broj upisa"));
        }
    }

    // =====================================================================
    // PROSJEK
    // =====================================================================

    @Nested
    @DisplayName("Prosjek")
    class Prosjek {

        @Test
        @DisplayName("Negativan prosjek se odbija i bez godine studija")
        void negativan() {
            Prijava p = prijava();
            p.setProsjek(new BigDecimal("-3.00"));
            assertTrue(pada(p).contains("negativan"));
        }

        @Test
        @DisplayName("Bez godine studija skala se ne provjerava")
        void bezGodine() {
            Prijava p = prijava();
            p.setProsjek(new BigDecimal("7.50"));
            prolazi(p);
        }

        @ParameterizedTest(name = "brucoš, prosjek {0} prolazi")
        @ValueSource(strings = {"1.00", "3.75", "5.00"})
        @DisplayName("Brucoš: prosjek srednje škole 1,00–5,00 prolazi")
        void brucosValidan(String pr) {
            Prijava p = prijava();
            p.setGodinaStudija(1);
            p.setProsjek(new BigDecimal(pr));
            prolazi(p);
        }

        @ParameterizedTest(name = "brucoš, prosjek {0} se odbija")
        @ValueSource(strings = {"0.00", "0.99", "5.01", "9.50"})
        @DisplayName("Brucoš: prosjek izvan 1,00–5,00 se odbija (pogrešna skala)")
        void brucosNevalidan(String pr) {
            Prijava p = prijava();
            p.setGodinaStudija(1);
            p.setProsjek(new BigDecimal(pr));
            assertTrue(pada(p).contains("srednje škole"));
        }

        @ParameterizedTest(name = "godina {0}, prosjek 6,00 i 10,00 prolaze")
        @ValueSource(ints = {2, 3, 7, 8})
        @DisplayName("Starije godine, apsolvent i postdiplomac: 6,00–10,00 prolazi")
        void fakultetValidan(int g) {
            Prijava p = prijava();
            p.setGodinaStudija(g);
            p.setProsjek(new BigDecimal("6.00"));
            prolazi(p);
            p.setProsjek(new BigDecimal("10.00"));
            prolazi(p);
        }

        @ParameterizedTest(name = "3. godina, prosjek {0} se odbija")
        @ValueSource(strings = {"4.00", "5.99", "10.01", "10.50"})
        @DisplayName("Starije godine: prosjek izvan 6,00–10,00 se odbija")
        void fakultetNevalidan(String pr) {
            Prijava p = prijava();
            p.setGodinaStudija(3);
            p.setProsjek(new BigDecimal(pr));
            assertTrue(pada(p).contains("6.00 i 10.00"));
        }

        @Test
        @DisplayName("Apsolvent sa srednjoškolskim prosjekom se odbija")
        void apsolventSrednjaSkala() {
            Prijava p = prijava();
            p.setGodinaStudija(7);
            p.setProsjek(new BigDecimal("4.00"));
            pada(p);
        }
    }

    // =====================================================================
    // ISPITI I UDALJENOST
    // =====================================================================

    @Nested
    @DisplayName("Ispiti i udaljenost")
    class IspitiIUdaljenost {

        @ParameterizedTest(name = "{0} ispita prolazi")
        @ValueSource(ints = {0, 1, 60, 100})
        @DisplayName("0–100 položenih ispita prolazi")
        void ispitiValidni(int i) {
            Prijava p = prijava();
            p.setPolozeniIspiti(i);
            prolazi(p);
        }

        @Test
        @DisplayName("Negativan broj ispita se odbija")
        void ispitiNegativni() {
            Prijava p = prijava();
            p.setPolozeniIspiti(-5);
            assertTrue(pada(p).contains("negativan"));
        }

        @ParameterizedTest(name = "{0} ispita se odbija")
        @ValueSource(ints = {101, 500})
        @DisplayName("Više od 100 ispita je nerealno")
        void ispitiPrevise(int i) {
            Prijava p = prijava();
            p.setPolozeniIspiti(i);
            assertTrue(pada(p).contains("nerealno"));
        }

        @ParameterizedTest(name = "{0} km prolazi")
        @ValueSource(strings = {"0", "0.5", "160", "1000"})
        @DisplayName("Udaljenost 0–1000 km prolazi (0 = student iz Zenice)")
        void udaljenostValidna(String km) {
            Prijava p = prijava();
            p.setUdaljenostKm(new BigDecimal(km));
            prolazi(p);
        }

        @Test
        @DisplayName("Negativna udaljenost se odbija")
        void udaljenostNegativna() {
            Prijava p = prijava();
            p.setUdaljenostKm(new BigDecimal("-50"));
            assertTrue(pada(p).contains("negativna"));
        }

        @ParameterizedTest(name = "{0} km se odbija")
        @ValueSource(strings = {"1000.01", "5000"})
        @DisplayName("Udaljenost veća od 1000 km je nerealna")
        void udaljenostPrevelika(String km) {
            Prijava p = prijava();
            p.setUdaljenostKm(new BigDecimal(km));
            assertTrue(pada(p).contains("nerealna"));
        }
    }

    // =====================================================================
    // DOMAĆINSTVO
    // =====================================================================

    @Nested
    @DisplayName("Domaćinstvo i primanja")
    class Domacinstvo {

        @ParameterizedTest(name = "{0} članova prolazi")
        @ValueSource(ints = {1, 4, 30})
        @DisplayName("1–30 članova prolazi (1 = sam student)")
        void clanoviValidni(int n) {
            Prijava p = new Prijava();
            p.setBrojClanovaDomacinstva(n);
            prolazi(p);
        }

        @ParameterizedTest(name = "{0} članova se odbija")
        @ValueSource(ints = {-3, 0})
        @DisplayName("Domaćinstvo bez članova se odbija")
        void clanoviPremalo(int n) {
            Prijava p = new Prijava();
            p.setBrojClanovaDomacinstva(n);
            assertTrue(pada(p).contains("najmanje jednog"));
        }

        @ParameterizedTest(name = "{0} članova se odbija")
        @ValueSource(ints = {31, 100})
        @DisplayName("Više od 30 članova je nerealno")
        void clanoviPrevise(int n) {
            Prijava p = new Prijava();
            p.setBrojClanovaDomacinstva(n);
            assertTrue(pada(p).contains("nerealan"));
        }

        @Test
        @DisplayName("Primanja 0 su dozvoljena")
        void primanjaNula() {
            Prijava p = prijava();
            p.setUkupnaPrimanja(BigDecimal.ZERO);
            prolazi(p);
        }

        @Test
        @DisplayName("Negativna primanja se odbijaju")
        void primanjaNegativna() {
            Prijava p = prijava();
            p.setUkupnaPrimanja(new BigDecimal("-200"));
            assertTrue(pada(p).contains("negativna"));
        }

        @Test
        @DisplayName("Primanja bez broja članova se odbijaju (ne mogu se podijeliti)")
        void primanjaBezClanova() {
            Prijava p = new Prijava();
            p.setUkupnaPrimanja(new BigDecimal("800"));
            assertTrue(pada(p).contains("broj članova"));
        }

        @Test
        @DisplayName("Broj članova bez primanja prolazi")
        void clanoviBezPrimanja() {
            prolazi(prijava());
        }
    }

    // =====================================================================
    // KRITERIJI
    // =====================================================================

    @Nested
    @DisplayName("Kriteriji")
    class Kriteriji {

        @ParameterizedTest(name = "{0}: 100% prolazi")
        @ValueSource(strings = {"STUDENT_RVI", "INVALID_RODITELJA"})
        @DisplayName("Postotak invalidnosti 100 prolazi")
        void postotak100(String sifra) {
            Prijava p = prijava();
            dodaj(p, kriterij(sifra), 100.0);
            prolazi(p);
        }

        @ParameterizedTest(name = "{0}: 100,5% se odbija")
        @ValueSource(strings = {"STUDENT_RVI", "INVALID_RODITELJA"})
        @DisplayName("Postotak invalidnosti preko 100 se odbija")
        void postotakPreko100(String sifra) {
            Prijava p = prijava();
            dodaj(p, kriterij(sifra), 100.5);
            assertTrue(pada(p).contains("100"));
        }

        @Test
        @DisplayName("Negativan parametar se odbija")
        void parametarNegativan() {
            Prijava p = prijava();
            dodaj(p, kriterij("STUDENT_RVI"), -20.0);
            assertTrue(pada(p).contains("negativan"));
        }

        @ParameterizedTest(name = "OS RBiH {0} mjeseci prolazi")
        @ValueSource(doubles = {0, 6, 44, 600})
        @DisplayName("OS RBiH: 0–600 mjeseci prolazi")
        void mjeseciValidni(double m) {
            Prijava p = prijava();
            dodaj(p, kriterij("DIJETE_OSRBIH"), m);
            prolazi(p);
        }

        @ParameterizedTest(name = "OS RBiH {0} mjeseci se odbija")
        @ValueSource(doubles = {601, 9999})
        @DisplayName("OS RBiH: više od 600 mjeseci je nerealno")
        void mjeseciPrevise(double m) {
            Prijava p = prijava();
            dodaj(p, kriterij("DIJETE_OSRBIH"), m);
            assertTrue(pada(p).contains("mjeseci"));
        }

        @Test
        @DisplayName("Broj mjeseci veći od 100 nije greška (to nije postotak)")
        void mjeseciNisuPostotak() {
            Prijava p = prijava();
            dodaj(p, kriterij("DIJETE_OSRBIH"), 150.0);
            prolazi(p);
        }

        @Test
        @DisplayName("Parametarski kriterij bez parametra prolazi (unosi se kasnije)")
        void bezParametra() {
            Prijava p = prijava();
            dodaj(p, kriterij("STUDENT_RVI"), null);
            prolazi(p);
        }

        @Test
        @DisplayName("Kod kriterija s fiksnim bodovima parametar se ne provjerava")
        void fiksniIgnoriseParametar() {
            Prijava p = prijava();
            dodaj(p, fiksni("IZBJEGLICA"), -5.0);
            prolazi(p);
        }

        @Test
        @DisplayName("Kriterij bez vrste se odbija")
        void kriterijBezVrste() {
            Prijava p = prijava();
            p.dodajKriterij(new PrijavaKriterij());
            assertTrue(pada(p).contains("vrste"));
        }
    }

    // ---------- pomoćne ----------

    private static VrstaKriterija kriterij(String sifra) {
        VrstaKriterija v = new VrstaKriterija();
        v.setSifra(sifra);
        v.setNaziv(sifra);
        v.setTraziParametar(true);
        v.setGrupa("BRANIOCI");
        return v;
    }

    private static VrstaKriterija fiksni(String sifra) {
        VrstaKriterija v = kriterij(sifra);
        v.setTraziParametar(false);
        v.setGrupa("DODATNI");
        return v;
    }

    private static void dodaj(Prijava p, VrstaKriterija v, Double param) {
        PrijavaKriterij k = new PrijavaKriterij();
        k.setVrsta(v);
        if (param != null) k.setParametar(BigDecimal.valueOf(param));
        p.dodajKriterij(k);
    }
}