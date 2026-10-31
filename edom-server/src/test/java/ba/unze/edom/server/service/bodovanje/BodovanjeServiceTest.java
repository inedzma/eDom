package ba.unze.edom.server.service.bodovanje;

import ba.unze.edom.common.dto.bodovanje.RezultatBodovanja;
import ba.unze.edom.common.dto.bodovanje.StavkaBodovanja;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.entity.VrstaKriterija;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import ba.unze.edom.server.service.opste.PostavkeService;
import ba.unze.edom.server.service.prijava.GodineStudija;
import ba.unze.edom.server.service.prijava.PrijavaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bodovanje prema Pravilniku Studentskog centra Zenica, akademska 2026/2027.
 * Svaki očekivani rezultat je izračunat ručno iz Pravilnika.
 *
 * Unit test bez Springa i baze: pokreće se za par milisekundi.
 */
class BodovanjeServiceTest {

    private static final double D = 0.001;

    private BodovanjeService servis;

    @BeforeEach
    void setUp() {
        servis = new BodovanjeService(new PrijavaValidator(), podrazumijevanePostavke());
    }

    /**
     * Postavke bez fajla → vrijede podrazumijevana pravila iz Pravilnika.
     * Test tako ne zavisi od data/postavke.properties na disku.
     */
    static PostavkeService podrazumijevanePostavke() {
        return new PostavkeService(
                "target/test-nema-postavki/postavke.properties",  // fajl ne postoji
                220, 0.35, "165,00", "", "01.10.", "15.07.");
    }

    // =====================================================================
    // USPJEH
    // =====================================================================

    @Nested
    @DisplayName("Uspjeh")
    class Uspjeh {

        @Test
        @DisplayName("Brucoš: prosjek × 9 + 1")
        void brucos() {
            Prijava p = prijava(1, "4.50", null);
            assertEquals(41.50, bodovi(p, "Uspjeh (brucoš)"), D);
        }

        @Test
        @DisplayName("3. godina: prosjek × 3,5 + ispiti × 1,8 + 5")
        void trecaGodina() {
            Prijava p = prijava(3, "8.50", 20);
            // 8,5 × 3,5 = 29,75   20 × 1,8 = 36   bonus 5
            assertEquals(70.75, bodovi(p, "Uspjeh"), D);
        }

        @Test
        @DisplayName("Apsolvent nema bonus za godinu")
        void apsolventBezBonusa() {
            Prijava p = prijava(GodineStudija.APSOLVENT, "8.00", 40);
            // 8 × 3,5 = 28   40 × 1,8 = 72   bonus 0
            assertEquals(100.00, bodovi(p, "Uspjeh"), D);
        }

        @Test
        @DisplayName("Postdiplomac ima bonus 10")
        void postdiplomac() {
            Prijava p = prijava(GodineStudija.POSTDIPLOMAC, "9.00", 10);
            // 9 × 3,5 = 31,5   10 × 1,8 = 18   bonus 10
            assertEquals(59.50, bodovi(p, "Uspjeh"), D);
        }

        @ParameterizedTest(name = "{0}. godina → bonus {1}")
        @CsvSource({"2,3", "3,5", "4,7", "5,9", "6,10", "7,0", "8,10"})
        @DisplayName("Bonus za godinu studija")
        void bonusi(int godina, double bonus) {
            assertEquals(bonus, servis.bonusGodine(godina), D);
        }

        @Test
        @DisplayName("Broj ispita nema gornju granicu: stariji studenti imaju prednost")
        void ispitiBezGranice() {
            Prijava p = prijava(5, "7.00", 60);
            // 7 × 3,5 = 24,5   60 × 1,8 = 108   bonus 9
            assertEquals(141.50, bodovi(p, "Uspjeh"), D);
        }
    }

    // =====================================================================
    // PONOVNI UPIS GODINE (pitanje 1)
    // =====================================================================

    @Nested
    @DisplayName("Ponovni upis godine")
    class PonovniUpis {

        @Test
        @DisplayName("Prvi put: bez kazne i bez upozorenja")
        void prviPut() {
            Prijava p = prijava(3, "8.50", 20);
            p.setUpisGodinePut(1);
            p.setUspjehVerifikovan(true);

            RezultatBodovanja r = servis.izracunaj(p);
            assertNull(stavka(r, "Ponovni upis godine"));
            assertFalse(r.imaUpozorenja());
            assertEquals(70.75, r.potvrdjeno(), D);
        }

        @Test
        @DisplayName("Drugi put: −3 boda")
        void drugiPut() {
            Prijava p = prijava(3, "8.50", 20);
            p.setUpisGodinePut(2);
            p.setUspjehVerifikovan(true);

            RezultatBodovanja r = servis.izracunaj(p);
            assertEquals(-3, stavka(r, "Ponovni upis godine").bodovi(), D);
            assertEquals(67.75, r.potvrdjeno(), D);
            assertFalse(r.imaUpozorenja());
        }

        @Test
        @DisplayName("Treći put: −3 boda, prijava se boduje, ali admin dobija upozorenje")
        void treciPut() {
            Prijava p = prijava(3, "8.50", 20);
            p.setUpisGodinePut(3);
            p.setUspjehVerifikovan(true);

            RezultatBodovanja r = servis.izracunaj(p);
            assertEquals(67.75, r.potvrdjeno(), D);
            assertTrue(r.imaUpozorenja());
            assertTrue(r.upozorenja().get(0).contains("3. put"));
        }

        @Test
        @DisplayName("Kazna se potvrđuje zajedno s uspjehom")
        void kaznaPratiVerifikacijuUspjeha() {
            Prijava p = prijava(3, "8.50", 20);
            p.setUpisGodinePut(2);
            p.setUspjehVerifikovan(false);

            RezultatBodovanja r = servis.izracunaj(p);
            assertEquals(0, r.potvrdjeno(), D);
            assertEquals(67.75, r.potencijalno(), D);
        }
    }

    // =====================================================================
    // UDALJENOST
    // =====================================================================

    @ParameterizedTest(name = "{0} km → {1} bodova")
    @CsvSource({
            "20,     0",
            "49.99,  0",
            "50,     4",
            "79.99,  4",
            "80,     8",
            "119.99, 8",
            "120,   12",
            "159.99,12",
            "160,   15",
            "400,   15"
    })
    @DisplayName("Udaljenost: granice razreda")
    void udaljenost(String km, double ocekivano) {
        Prijava p = prijava(3, "8.00", 10);
        p.setUdaljenostKm(new BigDecimal(km));
        assertEquals(ocekivano, bodovi(p, "Udaljenost"), D);
    }

    // =====================================================================
    // PRIMANJA PO ČLANU (student se broji u članove)
    // =====================================================================

    @ParameterizedTest(name = "{0} KM / {1} članova → {2} bodova")
    @CsvSource({
            "0,       4, 20",
            "436,     4, 20",   // 109 po članu
            "440,     4, 15",   // 110
            "600,     4, 15",   // 150
            "600.04,  4,  8",   // 150,01
            "800,     4,  8",   // 200
            "1200,    4,  4",   // 300
            "1600,    4,  2",   // 400
            "1600.04, 4,  0"    // 400,01
    })
    @DisplayName("Primanja po članu domaćinstva: granice razreda")
    void primanja(String ukupno, int clanova, double ocekivano) {
        Prijava p = prijava(3, "8.00", 10);
        p.setUkupnaPrimanja(new BigDecimal(ukupno));
        p.setBrojClanovaDomacinstva(clanova);
        assertEquals(ocekivano, bodovi(p, "Primanja domaćinstva"), D);
    }

    // =====================================================================
    // BORAČKE I SOCIJALNE KATEGORIJE
    // =====================================================================

    @Nested
    @DisplayName("Boračke i socijalne kategorije")
    class Branioci {

        @Test
        @DisplayName("Boduje se samo najviša kategorija")
        void samoNajvisa() {
            Prijava p = prijava(3, "8.00", 10);
            dodaj(p, fiksni("POGINULI_BRANILAC", 50, "BRANIOCI"), true);
            dodaj(p, fiksni("BEZ_JEDNOG_RODIT", 15, "BRANIOCI"), true);
            dodaj(p, fiksni("SOCIJALNA_POMOC", 10, "BRANIOCI"), true);

            RezultatBodovanja r = servis.izracunaj(p);
            long braniocaUStavkama = r.stavke().stream()
                    .filter(s -> "BRANIOCI".equals(s.kategorija())).count();
            assertEquals(1, braniocaUStavkama);
            assertEquals(50, stavka(r, "POGINULI_BRANILAC").bodovi(), D);
        }

        @Test
        @DisplayName("Potvrđeno uzima najvišu PROVJERENU kategoriju")
        void najvisaProvjerena() {
            Prijava p = prijava(3, "8.00", 10);
            p.setUspjehVerifikovan(true);                                  // 28 + 18 + 5 = 51
            dodaj(p, fiksni("POGINULI_BRANILAC", 50, "BRANIOCI"), false);  // nije provjereno
            dodaj(p, fiksni("BEZ_JEDNOG_RODIT", 15, "BRANIOCI"), true);

            RezultatBodovanja r = servis.izracunaj(p);
            assertEquals(51 + 15, r.potvrdjeno(), D);
            assertEquals(51 + 50, r.potencijalno(), D);
        }

        @ParameterizedTest(name = "RVI {0}% → {1} bodova")
        @CsvSource({"0,0", "30,15", "59,15", "60,20", "100,20"})
        @DisplayName("Student RVI")
        void rvi(String postotak, double ocekivano) {
            Prijava p = prijava(3, "8.00", 10);
            dodajSParametrom(p, parametarski("STUDENT_RVI"), postotak);
            assertEquals(ocekivano, bodovi(p, "STUDENT_RVI"), D);
        }

        @Test
        @DisplayName("Invalidnost roditelja: postotak × 0,1")
        void invalidnostRoditelja() {
            Prijava p = prijava(3, "8.00", 10);
            dodajSParametrom(p, parametarski("INVALID_RODITELJA"), "80");
            assertEquals(8, bodovi(p, "INVALID_RODITELJA"), D);
        }

        @Test
        @DisplayName("Nosilac ratnih priznanja: 10 bodova bez obzira na broj priznanja")
        void ratnaPriznanja() {
            Prijava p = prijava(3, "8.00", 10);
            VrstaKriterija v = fiksni("NOSILAC_PRIZNANJA", 10, "BRANIOCI");
            v.setTraziParametar(true);
            v.setNazivParametra("Broj ratnih priznanja");
            dodajSParametrom(p, v, "3");
            assertEquals(10, bodovi(p, "NOSILAC_PRIZNANJA"), D);
        }
    }

    // =====================================================================
    // OS RBiH: PERIOD, PUNI MJESECI × 0,3
    // =====================================================================

    @Nested
    @DisplayName("Učešće roditelja u OS RBiH")
    class OsRbih {

        @ParameterizedTest(name = "{0} – {1} → {2} punih mjeseci")
        @CsvSource({
                "1993-01-01, 1993-07-11,  6",   // 6 mjeseci i 10 dana
                "1993-01-01, 1993-09-30,  8",   // 8 mjeseci i 29 dana
                "1993-01-01, 1993-01-31,  0",   // manje od mjeseca
                "1992-04-15, 1995-12-23, 44"
        })
        @DisplayName("Broje se samo puni mjeseci")
        void puniMjeseci(String od, String doDatuma, long mjeseci) {
            assertEquals(mjeseci,
                    BodovanjeService.punihMjeseci(LocalDate.parse(od), LocalDate.parse(doDatuma)));
        }

        @Test
        @DisplayName("44 puna mjeseca → 13,2 boda (bez gornje granice)")
        void bodovi44() {
            Prijava p = prijava(3, "8.00", 10);
            dodajPeriod(p, "1992-04-15", "1995-12-23");
            assertEquals(13.2, bodovi(p, "DIJETE_OSRBIH"), D);
        }

        @Test
        @DisplayName("6 mjeseci i 10 dana → 1,8 bodova")
        void bodovi6() {
            Prijava p = prijava(3, "8.00", 10);
            dodajPeriod(p, "1993-01-01", "1993-07-11");
            assertEquals(1.8, bodovi(p, "DIJETE_OSRBIH"), D);
        }

        @Test
        @DisplayName("Datum kraja prije početka je greška")
        void obrnutiDatumi() {
            Prijava p = prijava(3, "8.00", 10);
            dodajPeriod(p, "1995-01-01", "1993-01-01");
            assertThrows(NevalidnaPrijavaException.class, () -> servis.izracunaj(p));
        }
    }

    // =====================================================================
    // DODATNI KRITERIJI (sabiraju se)
    // =====================================================================

    @Test
    @DisplayName("Dodatni kriteriji se sabiraju: izbjeglica 3 + brat/sestra 2 + nagrade 3")
    void dodatniSeSabiraju() {
        Prijava p = prijava(3, "8.00", 10);
        p.setUspjehVerifikovan(true);                              // 51
        dodaj(p, fiksni("IZBJEGLICA", 3, "DODATNI"), true);
        dodaj(p, fiksni("BRAT_SESTRA", 2, "DODATNI"), true);
        dodaj(p, fiksni("NAGRADE", 3, "DODATNI"), true);

        assertEquals(51 + 8, servis.izracunaj(p).potvrdjeno(), D);
    }

    // =====================================================================
    // CIJELA PRIJAVA
    // =====================================================================

    @Test
    @DisplayName("Cijela prijava, sve provjereno")
    void cijelaPrijava() {
        Prijava p = prijava(3, "8.50", 20);                         // uspjeh 70,75
        p.setUdaljenostKm(new BigDecimal("95"));                    // 8
        p.setUkupnaPrimanja(new BigDecimal("1200"));                // 1200 / 4 = 300 → 4
        p.setBrojClanovaDomacinstva(4);
        p.setUspjehVerifikovan(true);
        p.setUdaljenostVerifikovana(true);
        p.setPrimanjaVerifikovana(true);
        dodaj(p, fiksni("BEZ_JEDNOG_RODIT", 15, "BRANIOCI"), true); // 15
        dodaj(p, fiksni("IZBJEGLICA", 3, "DODATNI"), true);          // 3

        RezultatBodovanja r = servis.izracunaj(p);
        assertEquals(70.75 + 8 + 4 + 15 + 3, r.potvrdjeno(), D);
        assertEquals(r.potvrdjeno(), r.potencijalno(), D);
        assertFalse(r.cekaVerifikaciju());
    }

    @Test
    @DisplayName("Ništa provjereno: potvrđeno 0, potencijalno puno")
    void nistaProvjereno() {
        Prijava p = prijava(3, "8.50", 20);
        p.setUdaljenostKm(new BigDecimal("95"));

        RezultatBodovanja r = servis.izracunaj(p);
        assertEquals(0, r.potvrdjeno(), D);
        assertEquals(78.75, r.potencijalno(), D);
        assertTrue(r.cekaVerifikaciju());
    }

    // =====================================================================
    // pomoćne
    // =====================================================================

    private Prijava prijava(int godina, String prosjek, Integer ispiti) {
        Prijava p = new Prijava();
        p.setGodinaStudija(godina);
        p.setProsjek(new BigDecimal(prosjek));
        p.setPolozeniIspiti(ispiti);
        return p;
    }

    private VrstaKriterija fiksni(String sifra, double bodovi, String grupa) {
        VrstaKriterija v = new VrstaKriterija();
        v.setSifra(sifra);
        v.setNaziv(sifra);   // u testu naziv = šifra, da se stavka lako nađe
        v.setBodovi(BigDecimal.valueOf(bodovi));
        v.setGrupa(grupa);
        return v;
    }

    private VrstaKriterija parametarski(String sifra) {
        VrstaKriterija v = new VrstaKriterija();
        v.setSifra(sifra);
        v.setNaziv(sifra);
        v.setGrupa("BRANIOCI");
        v.setTraziParametar(true);
        v.setNazivParametra("Postotak");
        return v;
    }

    private void dodaj(Prijava p, VrstaKriterija v, boolean verifikovan) {
        PrijavaKriterij k = new PrijavaKriterij();
        k.setVrsta(v);
        k.setVerifikovan(verifikovan);
        p.dodajKriterij(k);
    }

    private void dodajSParametrom(Prijava p, VrstaKriterija v, String parametar) {
        PrijavaKriterij k = new PrijavaKriterij();
        k.setVrsta(v);
        k.setParametar(new BigDecimal(parametar));
        p.dodajKriterij(k);
    }

    private void dodajPeriod(Prijava p, String od, String doDatuma) {
        VrstaKriterija v = new VrstaKriterija();
        v.setSifra("DIJETE_OSRBIH");
        v.setNaziv("DIJETE_OSRBIH");
        v.setGrupa("BRANIOCI");
        v.setTraziPeriod(true);
        PrijavaKriterij k = new PrijavaKriterij();
        k.setVrsta(v);
        k.setDatumOd(LocalDate.parse(od));
        k.setDatumDo(LocalDate.parse(doDatuma));
        p.dodajKriterij(k);
    }

    private StavkaBodovanja stavka(RezultatBodovanja r, String naziv) {
        return r.stavke().stream().filter(s -> s.naziv().equals(naziv)).findFirst().orElse(null);
    }

    private double bodovi(Prijava p, String naziv) {
        StavkaBodovanja s = stavka(servis.izracunaj(p), naziv);
        assertNotNull(s, "Nema stavke '" + naziv + "'");
        return s.bodovi();
    }
}