package ba.unze.edom.server.service.bodovanje;

import ba.unze.edom.common.dto.opste.PostavkeDTO;
import ba.unze.edom.server.service.prijava.GodineStudija;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Čitanje pravila bodovanja koja admin upisuje u Postavkama (npr. "<50:0; <80:4; *:15").
 * Prazno polje znači podrazumijevano pravilo iz Pravilnika; neispravan unos
 * mora biti odbijen s porukom koja adminu kaže šta je pogrešno.
 *
 * PostavkeDTO je zamijenjen Mockito objektom: polja koja test ne zada su prazna (null).
 */
class PravilaBodovanjaTest {

    private static final double D = 0.001;

    private static PostavkeDTO prazne() {
        return mock(PostavkeDTO.class);
    }

    // =====================================================================
    // PODRAZUMIJEVANA PRAVILA (sva polja prazna)
    // =====================================================================

    @Nested
    @DisplayName("Prazne postavke → pravila iz Pravilnika")
    class Podrazumijevano {

        private final PravilaBodovanja pr = PravilaBodovanja.iz(prazne());

        @Test
        @DisplayName("Množioci za uspjeh i kazna za ponovni upis")
        void mnozioci() {
            assertEquals(9, pr.brucosMnozilac(), D);
            assertEquals(1, pr.brucosDodatak(), D);
            assertEquals(3.5, pr.prosjekMnozilac(), D);
            assertEquals(1.8, pr.ispitMnozilac(), D);
            assertEquals(-3, pr.kaznaPonovniUpis(), D);
        }

        @ParameterizedTest(name = "godina {0} → bonus {1}")
        @CsvSource({"1,0", "2,3", "3,5", "4,7", "5,9", "6,10"})
        @DisplayName("Bonus po godini (prva godina nema bonus)")
        void bonus(int godina, double ocekivano) {
            assertEquals(ocekivano, pr.bonus(godina), D);
        }

        @Test
        @DisplayName("Apsolvent 0, postdiplomac 10")
        void apsolventPostdiplomac() {
            assertEquals(0, pr.bonus(GodineStudija.APSOLVENT), D);
            assertEquals(10, pr.bonus(GodineStudija.POSTDIPLOMAC), D);
        }

        @ParameterizedTest(name = "{0} km → {1}")
        @CsvSource({"0,0", "49.99,0", "50,4", "80,8", "120,12", "159.99,12", "160,15", "1000,15"})
        @DisplayName("Skala udaljenosti")
        void udaljenost(double km, double ocekivano) {
            assertEquals(ocekivano, pr.udaljenost().bodovi(km), D);
        }

        @ParameterizedTest(name = "{0} KM po članu → {1}")
        @CsvSource({"0,20", "109.99,20", "110,15", "150,15", "150.01,8", "200,8", "300,4", "400,2", "400.01,0"})
        @DisplayName("Skala primanja (\"<=\" uključuje granicu, \"<\" ne)")
        void primanja(double poClanu, double ocekivano) {
            assertEquals(ocekivano, pr.primanja().bodovi(poClanu), D);
        }
    }

    // =====================================================================
    // PRAVILA KOJA ADMIN UPIŠE
    // =====================================================================

    @Nested
    @DisplayName("Pravila koja upiše admin")
    class Izmijenjena {

        @ParameterizedTest(name = "\"{0}\" → 3,5")
        @ValueSource(strings = {"3,5", "3.5", " 3,5 "})
        @DisplayName("Decimalni zarez i tačka, razmaci se zanemaruju")
        void decimalniZarez(String unos) {
            PostavkeDTO p = prazne();
            when(p.prosjekMnozilac()).thenReturn(unos);
            assertEquals(3.5, PravilaBodovanja.iz(p).prosjekMnozilac(), D);
        }

        @Test
        @DisplayName("Polje sa samo razmacima znači podrazumijevano")
        void prazanTekst() {
            PostavkeDTO p = prazne();
            when(p.brucosMnozilac()).thenReturn("   ");
            assertEquals(9, PravilaBodovanja.iz(p).brucosMnozilac(), D);
        }

        @Test
        @DisplayName("Nova skala udaljenosti se primjenjuje")
        void novaSkala() {
            PostavkeDTO p = prazne();
            when(p.udaljenostBodovi()).thenReturn("<30:0; <=100:10; *:20");
            var s = PravilaBodovanja.iz(p).udaljenost();
            assertEquals(0, s.bodovi(29.9), D);
            assertEquals(10, s.bodovi(30), D);
            assertEquals(10, s.bodovi(100), D);
            assertEquals(20, s.bodovi(100.01), D);
        }

        @Test
        @DisplayName("Bonus: samo navedene godine, ostale 0")
        void noviBonus() {
            PostavkeDTO p = prazne();
            when(p.bonusGodine()).thenReturn("3:6; Apsolvent:2; POSTDIPLOMAC:12");
            var pr = PravilaBodovanja.iz(p);
            assertEquals(6, pr.bonus(3), D);
            assertEquals(0, pr.bonus(4), D);
            assertEquals(2, pr.bonus(GodineStudija.APSOLVENT), D);
            assertEquals(12, pr.bonus(GodineStudija.POSTDIPLOMAC), D);
        }

        @Test
        @DisplayName("Kazna 0 isključuje kaznu za ponovni upis")
        void kaznaNula() {
            PostavkeDTO p = prazne();
            when(p.kaznaPonovniUpis()).thenReturn("0");
            assertEquals(0, PravilaBodovanja.iz(p).kaznaPonovniUpis(), D);
        }
    }

    // =====================================================================
    // NEISPRAVAN UNOS
    // =====================================================================

    @Nested
    @DisplayName("Neispravan unos se odbija s jasnom porukom")
    class Neispravno {

        private String greska(PostavkeDTO p) {
            return assertThrows(IllegalArgumentException.class, () -> PravilaBodovanja.iz(p)).getMessage();
        }

        @Test
        @DisplayName("Tekst umjesto broja")
        void nijeBroj() {
            PostavkeDTO p = prazne();
            when(p.brucosMnozilac()).thenReturn("devet");
            String g = greska(p);
            assertTrue(g.contains("Brucoš – množilac prosjeka"));
            assertTrue(g.contains("nije broj"));
        }

        @Test
        @DisplayName("Skala bez \"*\" za sve iznad zadnje granice")
        void bezZvjezdice() {
            PostavkeDTO p = prazne();
            when(p.udaljenostBodovi()).thenReturn("<50:0; <80:4");
            String g = greska(p);
            assertTrue(g.startsWith("Udaljenost"));
            assertTrue(g.contains("*:bodovi"));
        }

        @Test
        @DisplayName("Granica bez < ili <=")
        void granicaBezZnaka() {
            PostavkeDTO p = prazne();
            when(p.primanjaBodovi()).thenReturn("110:20; *:0");
            String g = greska(p);
            assertTrue(g.startsWith("Primanja po članu"));
            assertTrue(g.contains("mora počinjati sa < ili <="));
        }

        @Test
        @DisplayName("Granice poredane od veće prema manjoj")
        void pogresanRedoslijed() {
            PostavkeDTO p = prazne();
            when(p.udaljenostBodovi()).thenReturn("<100:8; <50:4; *:15");
            assertTrue(greska(p).contains("od manje prema većoj"));
        }

        @Test
        @DisplayName("Dio bez dvotačke")
        void bezDvotacke() {
            PostavkeDTO p = prazne();
            when(p.udaljenostBodovi()).thenReturn("<50-0; *:15");
            assertTrue(greska(p).contains("nije u obliku"));
        }

        @Test
        @DisplayName("Skala samo sa \"*\", bez ijedne granice")
        void bezGranica() {
            PostavkeDTO p = prazne();
            when(p.udaljenostBodovi()).thenReturn("*:15");
            assertTrue(greska(p).contains("bar jednu granicu"));
        }

        @Test
        @DisplayName("Bodovi u skali nisu broj")
        void bodoviNisuBroj() {
            PostavkeDTO p = prazne();
            when(p.udaljenostBodovi()).thenReturn("<50:nula; *:15");
            assertTrue(greska(p).contains("nije broj"));
        }

        @Test
        @DisplayName("Bonus za nepostojeću godinu")
        void nepoznataGodina() {
            PostavkeDTO p = prazne();
            when(p.bonusGodine()).thenReturn("treca:5");
            String g = greska(p);
            assertTrue(g.startsWith("Bonus za godinu"));
            assertTrue(g.contains("nije godina"));
        }
    }
}