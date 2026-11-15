package ba.unze.edom.server.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pravila jake lozinke: najmanje 8 i najviše 72 znaka, bez razmaka na krajevima,
 * te barem jedno veliko slovo, malo slovo, cifra i poseban znak.
 * Ista pravila važe za registraciju i za promjenu lozinke.
 */
class LozinkaPravilaTest {

    // =====================================================================
    // ISPRAVNE LOZINKE
    // =====================================================================

    @Nested
    @DisplayName("Ispravne lozinke")
    class Ispravne {

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {
                "NovaLozinka1!",
                "Ab1!abcd",            // tačno 8 znakova
                "Čvrsta1!",            // naša slova se računaju kao slova
                "Zenica2026#dom",
                "a1B@a1B@a1B@",
                "Lozinka sa razmakom1!" // razmak u sredini je dozvoljen
        })
        @DisplayName("Prolaze bez ijedne greške")
        void prolaze(String lozinka) {
            assertTrue(LozinkaPravila.greske(lozinka).isEmpty(),
                    () -> "Odbijena: " + LozinkaPravila.greske(lozinka));
            assertNull(LozinkaPravila.poruka(lozinka));
        }

        @Test
        @DisplayName("Tačno 72 znaka (granica BCrypt-a) prolazi")
        void granica72() {
            String l = "Ab1!" + "a".repeat(68);
            assertEquals(72, l.length());
            assertTrue(LozinkaPravila.greske(l).isEmpty());
        }
    }

    // =====================================================================
    // PRAZNA LOZINKA
    // =====================================================================

    @ParameterizedTest(name = "\"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Prazna lozinka: samo jedna poruka, 'lozinka je obavezna'")
    void prazna(String lozinka) {
        assertEquals(List.of("lozinka je obavezna"), LozinkaPravila.greske(lozinka));
    }

    // =====================================================================
    // NEDOSTAJE PO JEDAN USLOV
    // =====================================================================

    @Nested
    @DisplayName("Nedostaje tačno jedan uslov")
    class JedanUslov {

        @Test
        @DisplayName("Nema velikog slova")
        void nemaVelikog() {
            assertEquals(List.of("jedno veliko slovo"), LozinkaPravila.greske("novalozinka1!"));
        }

        @Test
        @DisplayName("Nema malog slova")
        void nemaMalog() {
            assertEquals(List.of("jedno malo slovo"), LozinkaPravila.greske("NOVALOZINKA1!"));
        }

        @Test
        @DisplayName("Nema cifre")
        void nemaCifre() {
            assertEquals(List.of("jednu cifru"), LozinkaPravila.greske("NovaLozinka!"));
        }

        @Test
        @DisplayName("Nema posebnog znaka")
        void nemaPosebnog() {
            assertEquals(List.of("jedan poseban znak"), LozinkaPravila.greske("NovaLozinka1"));
        }

        @Test
        @DisplayName("Prekratka: 7 znakova")
        void prekratka() {
            assertEquals(List.of("najmanje 8 znakova"), LozinkaPravila.greske("Ab1!abc"));
        }

        @Test
        @DisplayName("Preduga: 73 znaka")
        void preduga() {
            String l = "Ab1!" + "a".repeat(69);
            assertEquals(73, l.length());
            assertEquals(List.of("najviše 72 znakova"), LozinkaPravila.greske(l));
        }

        @Test
        @DisplayName("Razmak na početku")
        void razmakNaPocetku() {
            assertEquals(List.of("bez razmaka na početku i kraju"),
                    LozinkaPravila.greske(" NovaLozinka1!"));
        }

        @Test
        @DisplayName("Razmak na kraju")
        void razmakNaKraju() {
            assertEquals(List.of("bez razmaka na početku i kraju"),
                    LozinkaPravila.greske("NovaLozinka1! "));
        }
    }

    // =====================================================================
    // VIŠE GREŠAKA ODJEDNOM
    // =====================================================================

    @Nested
    @DisplayName("Više grešaka odjednom")
    class ViseGresaka {

        @Test
        @DisplayName("Samo mala slova: fale veliko slovo, cifra i poseban znak")
        void samoMalaSlova() {
            assertEquals(List.of("jedno veliko slovo", "jednu cifru", "jedan poseban znak"),
                    LozinkaPravila.greske("lozinkalozinka"));
        }

        @Test
        @DisplayName("Samo cifre: fale velika i mala slova i poseban znak")
        void samoCifre() {
            assertEquals(List.of("jedno veliko slovo", "jedno malo slovo", "jedan poseban znak"),
                    LozinkaPravila.greske("12345678"));
        }

        @Test
        @DisplayName("Kratka i bez cifre i posebnog znaka")
        void kratkaISlaba() {
            assertEquals(List.of("najmanje 8 znakova", "jednu cifru", "jedan poseban znak"),
                    LozinkaPravila.greske("Abc"));
        }

        @Test
        @DisplayName("Česta lozinka 'lozinka123' ne prolazi")
        void cestaLozinka() {
            assertEquals(List.of("jedno veliko slovo", "jedan poseban znak"),
                    LozinkaPravila.greske("lozinka123"));
        }

        @Test
        @DisplayName("Samo posebni znakovi: fale slova i cifra")
        void samoPosebni() {
            assertEquals(List.of("jedno veliko slovo", "jedno malo slovo", "jednu cifru"),
                    LozinkaPravila.greske("!@#$%^&*"));
        }
    }

    // =====================================================================
    // PORUKA ZA KORISNIKA
    // =====================================================================

    @Test
    @DisplayName("Poruka nabraja sve što nedostaje u jednoj rečenici")
    void poruka() {
        assertEquals("Lozinka mora sadržavati: jedno veliko slovo, jednu cifru, jedan poseban znak.",
                LozinkaPravila.poruka("lozinkalozinka"));
    }

    @Test
    @DisplayName("Znak koji nije na listi posebnih (npr. €) se ne računa kao poseban")
    void znakVanListe() {
        assertEquals(List.of("jedan poseban znak"), LozinkaPravila.greske("NovaLozinka1€"));
    }
}