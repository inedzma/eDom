package ba.unze.edom.server.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validacija forme za registraciju (anotacije u RegistracijaZahtjev).
 * Svako polje se provjerava posebno: prazno, pogrešnog formata, predugo.
 * Ne treba ni Spring ni baza: validator se pravi direktno.
 */
class RegistracijaZahtjevValidacijaTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void pripremi() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void zatvori() {
        factory.close();
    }

    // =====================================================================
    // ISPRAVAN ZAHTJEV
    // =====================================================================

    @Test
    @DisplayName("Ispravno popunjena forma nema nijednu grešku")
    void ispravan() {
        assertTrue(greske(new Z().build()).isEmpty(), () -> greske(new Z().build()).toString());
    }

    @Test
    @DisplayName("Neobavezna polja (indeks, telefon) mogu ostati prazna")
    void neobaveznaPolja() {
        Z z = new Z();
        z.brojIndeksa = null;
        z.telefon = null;
        assertTrue(greske(z.build()).isEmpty());
    }

    @Test
    @DisplayName("Potpuno prazna forma: greška na svim obaveznim poljima")
    void praznaForma() {
        RegistracijaZahtjev prazan = new RegistracijaZahtjev(
                "", "", "", "", "", "", "", null, "", "", "", false);

        assertEquals(
                Set.of("ime", "prezime", "jmbg", "email", "fakultet", "godinaStudija",
                        "adresa", "imeRoditelja", "lozinka", "saglasnost"),
                greske(prazan).keySet());
    }

    // =====================================================================
    // IME, PREZIME, IME RODITELJA, ADRESA
    // =====================================================================

    @Nested
    @DisplayName("Lični podaci")
    class LicniPodaci {

        @ParameterizedTest(name = "ime = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Ime je obavezno")
        void ime(String ime) {
            Z z = new Z(); z.ime = ime;
            assertPoruka(z, "ime", "Ime je obavezno");
        }

        @ParameterizedTest(name = "prezime = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Prezime je obavezno")
        void prezime(String prezime) {
            Z z = new Z(); z.prezime = prezime;
            assertPoruka(z, "prezime", "Prezime je obavezno");
        }

        @ParameterizedTest(name = "ime roditelja = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Ime roditelja je obavezno")
        void imeRoditelja(String ime) {
            Z z = new Z(); z.imeRoditelja = ime;
            assertPoruka(z, "imeRoditelja", "Ime roditelja je obavezno");
        }

        @ParameterizedTest(name = "adresa = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Adresa je obavezna")
        void adresa(String adresa) {
            Z z = new Z(); z.adresa = adresa;
            assertPoruka(z, "adresa", "Adresa je obavezna");
        }

        @Test
        @DisplayName("Ime: 50 znakova prolazi, 51 ne prolazi")
        void imeDuzina() {
            Z dobro = new Z(); dobro.ime = "A".repeat(50);
            Z lose = new Z();  lose.ime = "A".repeat(51);
            assertFalse(greske(dobro.build()).containsKey("ime"));
            assertTrue(greske(lose.build()).containsKey("ime"));
        }

        @Test
        @DisplayName("Prezime duže od 50 znakova ne prolazi")
        void prezimeDuzina() {
            Z z = new Z(); z.prezime = "B".repeat(51);
            assertTrue(greske(z.build()).containsKey("prezime"));
        }

        @Test
        @DisplayName("Ime roditelja: 20 znakova prolazi, 21 ne prolazi")
        void imeRoditeljaDuzina() {
            Z dobro = new Z(); dobro.imeRoditelja = "C".repeat(20);
            Z lose = new Z();  lose.imeRoditelja = "C".repeat(21);
            assertFalse(greske(dobro.build()).containsKey("imeRoditelja"));
            assertTrue(greske(lose.build()).containsKey("imeRoditelja"));
        }

        @Test
        @DisplayName("Adresa duža od 50 znakova ne prolazi")
        void adresaDuzina() {
            Z z = new Z(); z.adresa = "D".repeat(51);
            assertTrue(greske(z.build()).containsKey("adresa"));
        }

        @Test
        @DisplayName("Imena s našim slovima i crticom su ispravna")
        void nasaSlova() {
            Z z = new Z();
            z.ime = "Šejla"; z.prezime = "Čaušević-Đulić"; z.imeRoditelja = "Ćazim";
            assertTrue(greske(z.build()).isEmpty());
        }
    }

    // =====================================================================
    // JMBG
    // =====================================================================

    @Nested
    @DisplayName("JMBG")
    class JmbgPolje {

        @ParameterizedTest(name = "jmbg = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("JMBG je obavezan")
        void prazan(String jmbg) {
            Z z = new Z(); z.jmbg = jmbg;
            assertPoruka(z, "jmbg", "JMBG je obavezan");
        }

        @ParameterizedTest(name = "jmbg = \"{0}\"")
        @ValueSource(strings = {
                "123",              // prekratak
                "010100317500",     // 12 cifara
                "01010031750071",   // 14 cifara
                "01010031750a7",    // slovo
                "0101003-175007",   // crtica
                " 0101003175007"    // razmak
        })
        @DisplayName("JMBG mora imati tačno 13 cifara")
        void pogresanFormat(String jmbg) {
            Z z = new Z(); z.jmbg = jmbg;
            assertPoruka(z, "jmbg", "JMBG mora imati 13 cifara");
        }
    }

    // =====================================================================
    // E-MAIL
    // =====================================================================

    @Nested
    @DisplayName("E-mail")
    class EmailPolje {

        @ParameterizedTest(name = "email = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("E-mail je obavezan")
        void prazan(String email) {
            Z z = new Z(); z.email = email;
            assertPoruka(z, "email", "E-mail je obavezan");
        }

        @ParameterizedTest(name = "email = \"{0}\"")
        @ValueSource(strings = {
                "merjem",                  // bez @
                "merjem@",                 // bez domene
                "@example.com",            // bez imena
                "merjem@@example.com",     // dva @
                "mer jem@example.com",     // razmak
                "merjem@example",          // bez .ba / .com
                "merjem@example.c"         // domena od jednog slova
        })
        @DisplayName("Pogrešno napisan e-mail se odbija")
        void pogresanFormat(String email) {
            Z z = new Z(); z.email = email;
            assertPoruka(z, "email", "Neispravan format e-mail adrese");
        }

        @ParameterizedTest(name = "email = \"{0}\"")
        @ValueSource(strings = {
                "merjem@example.com",
                "ime.prezime@unze.ba",
                "student+dom@gmail.com",
                "a_b-c@mail.student.unze.ba"
        })
        @DisplayName("Ispravni e-mailovi prolaze")
        void ispravni(String email) {
            Z z = new Z(); z.email = email;
            assertFalse(greske(z.build()).containsKey("email"));
        }
    }

    // =====================================================================
    // STUDIJ
    // =====================================================================

    @Nested
    @DisplayName("Podaci o studiju")
    class Studij {

        @ParameterizedTest(name = "fakultet = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Fakultet je obavezan")
        void fakultet(String fakultet) {
            Z z = new Z(); z.fakultet = fakultet;
            assertPoruka(z, "fakultet", "Fakultet je obavezan");
        }

        @Test
        @DisplayName("Godina studija je obavezna")
        void godinaObavezna() {
            Z z = new Z(); z.godinaStudija = null;
            assertPoruka(z, "godinaStudija", "Godina studija je obavezna");
        }

        @ParameterizedTest(name = "godina = {0}")
        @ValueSource(ints = {0, -1, 9, 100})
        @DisplayName("Godina izvan 1–8 se odbija")
        void godinaIzvanRaspona(int godina) {
            Z z = new Z(); z.godinaStudija = godina;
            assertTrue(greske(z.build()).containsKey("godinaStudija"));
        }

        @ParameterizedTest(name = "godina = {0}")
        @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
        @DisplayName("Godine 1–8 (uključujući apsolventa i postdiplomca) prolaze")
        void godinaURasponu(int godina) {
            Z z = new Z(); z.godinaStudija = godina;
            assertFalse(greske(z.build()).containsKey("godinaStudija"));
        }

        @Test
        @DisplayName("Broj indeksa duži od 20 znakova ne prolazi")
        void indeksDuzina() {
            Z z = new Z(); z.brojIndeksa = "1".repeat(21);
            assertTrue(greske(z.build()).containsKey("brojIndeksa"));
        }

        @Test
        @DisplayName("Telefon duži od 100 znakova ne prolazi")
        void telefonDuzina() {
            Z z = new Z(); z.telefon = "0".repeat(101);
            assertTrue(greske(z.build()).containsKey("telefon"));
        }
    }

    // =====================================================================
    // LOZINKA I SAGLASNOST
    // =====================================================================

    @Nested
    @DisplayName("Lozinka i saglasnost")
    class LozinkaISaglasnost {

        @ParameterizedTest(name = "lozinka = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {
                "lozinka123",       // nema velikog slova ni posebnog znaka
                "NOVALOZINKA1!",    // nema malog slova
                "NovaLozinka!",     // nema cifre
                "NovaLozinka1",     // nema posebnog znaka
                "Ab1!abc",          // 7 znakova
                " NovaLozinka1!"    // razmak na početku
        })
        @DisplayName("Slaba lozinka se odbija")
        void slabaLozinka(String lozinka) {
            Z z = new Z(); z.lozinka = lozinka;
            assertTrue(greske(z.build()).containsKey("lozinka"),
                    "Lozinka '" + lozinka + "' je prošla, a ne bi smjela");
        }

        @Test
        @DisplayName("Bez saglasnosti za obradu ličnih podataka registracija nije moguća")
        void bezSaglasnosti() {
            Z z = new Z(); z.saglasnost = false;
            assertPoruka(z, "saglasnost", "Morate dati saglasnost za obradu ličnih podataka.");
        }
    }

    // =====================================================================
    // pomoćne
    // =====================================================================

    /** Polje → poruke grešaka za to polje. */
    private static Map<String, Set<String>> greske(RegistracijaZahtjev z) {
        Set<ConstraintViolation<RegistracijaZahtjev>> v = validator.validate(z);
        return v.stream().collect(Collectors.groupingBy(
                c -> c.getPropertyPath().toString(),
                Collectors.mapping(ConstraintViolation::getMessage, Collectors.toSet())));
    }

    /** Provjerava da je greška baš na tom polju i da je poruka ona koju korisnik vidi. */
    private static void assertPoruka(Z z, String polje, String poruka) {
        var g = greske(z.build());
        assertTrue(g.containsKey(polje), () -> "Nema greške na polju '" + polje + "': " + g);
        assertTrue(g.get(polje).contains(poruka),
                () -> "Polje '" + polje + "' ima poruke " + g.get(polje) + ", očekivano: " + poruka);
    }

    /** Ispravno popunjena forma; test promijeni samo polje koje provjerava. */
    private static class Z {
        String ime = "Merjem";
        String prezime = "Grizić";
        String jmbg = "0101003175007";
        String brojIndeksa = "320";
        String email = "merjem@example.com";
        String telefon = "061111222";
        String fakultet = "Drugo";
        Integer godinaStudija = 3;
        String adresa = "Zenica, Ulica 1";
        String imeRoditelja = "Adem";
        String lozinka = "NovaLozinka1!";
        boolean saglasnost = true;

        RegistracijaZahtjev build() {
            return new RegistracijaZahtjev(ime, prezime, jmbg, brojIndeksa, email, telefon,
                    fakultet, godinaStudija, adresa, imeRoditelja, lozinka, saglasnost);
        }
    }
}