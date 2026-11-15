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
import ba.unze.edom.server.service.prijava.Statusi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bodovanje prema kriterijima Studentskog centra Zenica, akademska 2026/2027.
 * Svaki očekivani rezultat je izračunat ručno iz pravila.
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
     * Postavke bez fajla → vrijede podrazumijevana pravila.
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

        @Test
        @DisplayName("Bez prosjeka uspjeh je 0, ali prijava se boduje")
        void bezProsjeka() {
            Prijava p = new Prijava();
            p.setGodinaStudija(3);
            assertEquals(0, bodovi(p, "Uspjeh"), D);
        }
    }

    // =====================================================================
    // PONOVNI UPIS GODINE
    // =====================================================================

    @Nested
    @DisplayName("Ponovni upis godine")
    class PonovniUpis {

        @Test
        @DisplayName("Prvi put: bez kazne i bez upozorenja")
        void prviPut() {
            Prijava p = prijava(3, "8.50", 20);
            p.setUpisGodinePut(1);

            RezultatBodovanja r = servis.izracunaj(p);
            assertNull(stavka(r, "Ponovni upis godine"));
            assertFalse(r.imaUpozorenja());
            assertEquals(70.75, r.bodovi(), D);
        }

        @Test
        @DisplayName("Drugi put: −3 boda")
        void drugiPut() {
            Prijava p = prijava(3, "8.50", 20);
            p.setUpisGodinePut(2);

            RezultatBodovanja r = servis.izracunaj(p);
            assertEquals(-3, stavka(r, "Ponovni upis godine").bodovi(), D);
            assertEquals(67.75, r.bodovi(), D);
            assertFalse(r.imaUpozorenja());
        }

        @Test
        @DisplayName("Treći put: −3 boda, prijava se boduje, ali admin dobija upozorenje")
        void treciPut() {
            Prijava p = prijava(3, "8.50", 20);
            p.setUpisGodinePut(3);

            RezultatBodovanja r = servis.izracunaj(p);
            assertEquals(67.75, r.bodovi(), D);
            assertTrue(r.imaUpozorenja());
            assertTrue(r.upozorenja().get(0).contains("3. put"));
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
            Prijava p = prijava(3, "8.00", 10);                         // uspjeh 51
            dodaj(p, fiksni("POGINULI_BRANILAC", 50, "BRANIOCI"));
            dodaj(p, fiksni("BEZ_JEDNOG_RODIT", 15, "BRANIOCI"));
            dodaj(p, fiksni("SOCIJALNA_POMOC", 10, "BRANIOCI"));

            RezultatBodovanja r = servis.izracunaj(p);
            long braniocaUStavkama = r.stavke().stream()
                    .filter(s -> "BRANIOCI".equals(s.kategorija())).count();
            assertEquals(1, braniocaUStavkama);
            assertEquals(50, stavka(r, "POGINULI_BRANILAC").bodovi(), D);
            assertEquals(51 + 50, r.bodovi(), D);
        }

        @Test
        @DisplayName("Uklanjanjem nepotkrijepljenog kriterija boduje se sljedeći najviši")
        void uklanjanjeKriterija() {
            Prijava p = prijava(3, "8.00", 10);                         // uspjeh 51
            dodaj(p, fiksni("POGINULI_BRANILAC", 50, "BRANIOCI"));
            dodaj(p, fiksni("BEZ_JEDNOG_RODIT", 15, "BRANIOCI"));

            // sekretar uklanja kriterij za koji nije dostavljen dokaz
            p.getKriteriji().removeIf(k -> "POGINULI_BRANILAC".equals(k.getVrsta().getSifra()));

            assertEquals(51 + 15, servis.izracunaj(p).bodovi(), D);
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
    // OS RBiH: BROJ PUNIH MJESECI × 0,3
    // (period od–do pretvara se u broj mjeseci već na formi, pa se ovdje boduje broj)
    // =====================================================================

    @Nested
    @DisplayName("Učešće roditelja u OS RBiH")
    class OsRbih {

        @ParameterizedTest(name = "{0} mjeseci → {1} bodova")
        @CsvSource({"0, 0", "6, 1.8", "44, 13.2", "48, 14.4"})
        @DisplayName("Puni mjeseci × 0,3, bez gornje granice")
        void mjeseci(String mjeseci, double ocekivano) {
            Prijava p = prijava(3, "8.00", 10);
            dodajSParametrom(p, osRbih(), mjeseci);
            assertEquals(ocekivano, bodovi(p, "DIJETE_OSRBIH"), D);
        }

        @Test
        @DisplayName("Započeti mjesec se ne računa")
        void zapocetiMjesec() {
            Prijava p = prijava(3, "8.00", 10);
            dodajSParametrom(p, osRbih(), "6.9");
            assertEquals(1.8, bodovi(p, "DIJETE_OSRBIH"), D);
        }

        @Test
        @DisplayName("Nerealan broj mjeseci je greška")
        void nerealno() {
            Prijava p = prijava(3, "8.00", 10);
            dodajSParametrom(p, osRbih(), "9999");
            assertThrows(NevalidnaPrijavaException.class, () -> servis.izracunaj(p));
        }
    }

    // =====================================================================
    // DODATNI KRITERIJI (sabiraju se)
    // =====================================================================

    @Test
    @DisplayName("Dodatni kriteriji se sabiraju: izbjeglica 3 + brat/sestra 2 + nagrade 3")
    void dodatniSeSabiraju() {
        Prijava p = prijava(3, "8.00", 10);                              // 51
        dodaj(p, fiksni("IZBJEGLICA", 3, "DODATNI"));
        dodaj(p, fiksni("BRAT_SESTRA", 2, "DODATNI"));
        dodaj(p, fiksni("NAGRADE", 3, "DODATNI"));

        assertEquals(51 + 8, servis.izracunaj(p).bodovi(), D);
    }

    @Test
    @DisplayName("Isti dodatni kriterij dvaput se boduje samo jednom")
    void dodatniBezDupliranja() {
        Prijava p = prijava(3, "8.00", 10);                              // 51
        dodaj(p, fiksni("NAGRADE", 3, "DODATNI"));
        dodaj(p, fiksni("NAGRADE", 3, "DODATNI"));

        assertEquals(51 + 3, servis.izracunaj(p).bodovi(), D);
    }

    // =====================================================================
    // CIJELA PRIJAVA
    // =====================================================================

    @Test
    @DisplayName("Cijela prijava: zbir svih stavki")
    void cijelaPrijava() {
        Prijava p = prijava(3, "8.50", 20);                         // uspjeh 70,75
        p.setUdaljenostKm(new BigDecimal("95"));                    // 8
        p.setUkupnaPrimanja(new BigDecimal("1200"));                // 1200 / 4 = 300 → 4
        p.setBrojClanovaDomacinstva(4);
        dodaj(p, fiksni("BEZ_JEDNOG_RODIT", 15, "BRANIOCI"));       // 15
        dodaj(p, fiksni("IZBJEGLICA", 3, "DODATNI"));               // 3

        RezultatBodovanja r = servis.izracunaj(p);
        assertEquals(70.75 + 8 + 4 + 15 + 3, r.bodovi(), D);
    }

    @Test
    @DisplayName("Ukupno je jednako zbiru stavki")
    void ukupnoJeZbirStavki() {
        Prijava p = prijava(4, "7.80", 30);
        p.setUpisGodinePut(2);
        p.setUdaljenostKm(new BigDecimal("130"));
        p.setUkupnaPrimanja(new BigDecimal("500"));
        p.setBrojClanovaDomacinstva(5);
        dodaj(p, fiksni("BEZ_OBA_RODITELJA", 20, "BRANIOCI"));
        dodaj(p, fiksni("BRAT_SESTRA", 2, "DODATNI"));

        RezultatBodovanja r = servis.izracunaj(p);
        double zbir = r.stavke().stream().mapToDouble(StavkaBodovanja::bodovi).sum();
        assertEquals(zbir, r.bodovi(), D);
    }

    @Test
    @DisplayName("Neispravni podaci se ne boduju nego prijavljuju grešku")
    void neispravnaPrijava() {
        Prijava p = prijava(3, "11.00", 20);   // prosjek iznad 10
        assertThrows(NevalidnaPrijavaException.class, () -> servis.izracunaj(p));
    }

    @Test
    @DisplayName("Bez prijave rezultat je prazan")
    void nullPrijava() {
        RezultatBodovanja r = servis.izracunaj(null);
        assertEquals(0, r.bodovi(), D);
        assertTrue(r.stavke().isEmpty());
    }


    // =====================================================================
    // GRANICE FORMULA I ZAOKRUŽIVANJE
    // =====================================================================

    @Nested
    @DisplayName("Granice formula")
    class GraniceFormula {

        @ParameterizedTest(name = "brucoš, prosjek {0} → {1}")
        @CsvSource({"1.00, 10", "2.50, 23.5", "5.00, 46"})
        @DisplayName("Brucoš: najmanji i najveći mogući prosjek")
        void brucosGranice(String prosjek, double ocekivano) {
            assertEquals(ocekivano, bodovi(prijava(1, prosjek, null), "Uspjeh (brucoš)"), D);
        }

        @Test
        @DisplayName("Brucošu se ispiti ne računaju čak i ako su uneseni")
        void brucosIspitiSeNeRacunaju() {
            assertEquals(41.50, bodovi(prijava(1, "4.50", 12), "Uspjeh (brucoš)"), D);
        }

        @Test
        @DisplayName("Bez broja ispita računa se 0 ispita")
        void bezIspita() {
            // 8 × 3,5 = 28 + bonus 5
            assertEquals(33.0, bodovi(prijava(3, "8.00", null), "Uspjeh"), D);
        }

        @ParameterizedTest(name = "invalidnost roditelja {0}% → {1}")
        @CsvSource({"0, 0", "55, 5.5", "100, 10"})
        @DisplayName("Invalidnost roditelja: granice")
        void invalidnostGranice(String postotak, double ocekivano) {
            Prijava p = prijava(3, "8.00", 10);
            dodajSParametrom(p, parametarski("INVALID_RODITELJA"), postotak);
            assertEquals(ocekivano, bodovi(p, "INVALID_RODITELJA"), D);
        }

        @Test
        @DisplayName("Bodovi se zaokružuju na dvije decimale (7 mjeseci × 0,3 = 2,1)")
        void zaokruzivanje() {
            Prijava p = prijava(3, "8.00", 10);
            dodajSParametrom(p, osRbih(), "7");
            assertEquals(2.1, bodovi(p, "DIJETE_OSRBIH"), 0.0);
        }
    }

    // =====================================================================
    // KOMBINACIJE KRITERIJA
    // =====================================================================

    @Nested
    @DisplayName("Kombinacije kriterija")
    class Kombinacije {

        @Test
        @DisplayName("RVI 60% (20) i bez jednog roditelja (15): boduje se samo 20")
        void rviIFiksni() {
            Prijava p = prijava(3, "8.00", 10);                         // 51
            dodajSParametrom(p, parametarski("STUDENT_RVI"), "60");
            dodaj(p, fiksni("BEZ_JEDNOG_RODIT", 15, "BRANIOCI"));
            assertEquals(51 + 20, servis.izracunaj(p).bodovi(), D);
        }

        @Test
        @DisplayName("OS RBiH 44 mjeseca (13,2) i bez jednog roditelja (15): boduje se samo 15")
        void osRbihManjiOdFiksnog() {
            Prijava p = prijava(3, "8.00", 10);
            dodajSParametrom(p, osRbih(), "44");
            dodaj(p, fiksni("BEZ_JEDNOG_RODIT", 15, "BRANIOCI"));
            assertEquals(51 + 15, servis.izracunaj(p).bodovi(), D);
        }

        @Test
        @DisplayName("OS RBiH 60 mjeseci (18) i bez jednog roditelja (15): boduje se samo 18")
        void osRbihVeciOdFiksnog() {
            Prijava p = prijava(3, "8.00", 10);
            dodajSParametrom(p, osRbih(), "60");
            dodaj(p, fiksni("BEZ_JEDNOG_RODIT", 15, "BRANIOCI"));
            assertEquals(51 + 18, servis.izracunaj(p).bodovi(), D);
        }

        @Test
        @DisplayName("Najviši branilac i dodatni kriteriji se sabiraju")
        void braniocIDodatni() {
            Prijava p = prijava(3, "8.00", 10);
            dodaj(p, fiksni("BEZ_OBA_RODITELJA", 20, "BRANIOCI"));
            dodaj(p, fiksni("BEZ_JEDNOG_RODIT", 15, "BRANIOCI"));
            dodaj(p, fiksni("IZBJEGLICA", 3, "DODATNI"));
            dodaj(p, fiksni("BRAT_SESTRA", 2, "DODATNI"));
            assertEquals(51 + 20 + 3 + 2, servis.izracunaj(p).bodovi(), D);
        }

        @Test
        @DisplayName("Kriterij nepoznate grupe se ne boduje")
        void nepoznataGrupa() {
            Prijava p = prijava(3, "8.00", 10);
            dodaj(p, fiksni("NESTO", 50, "NEPOZNATA"));
            assertEquals(51, servis.izracunaj(p).bodovi(), D);
        }

        @Test
        @DisplayName("Parametarski kriterij bez parametra nosi 0 bodova")
        void parametarskiBezParametra() {
            Prijava p = prijava(3, "8.00", 10);
            dodaj(p, parametarski("STUDENT_RVI"));
            assertEquals(0, bodovi(p, "STUDENT_RVI"), D);
        }
    }

    // =====================================================================
    // NEUNESENI PODACI I STATUS
    // =====================================================================

    @Nested
    @DisplayName("Neuneseni podaci i status")
    class NeuneseniPodaci {

        @Test
        @DisplayName("Bez udaljenosti: stavka postoji s 0 bodova")
        void bezUdaljenosti() {
            assertEquals(0, bodovi(prijava(3, "8.00", 10), "Udaljenost"), D);
        }

        @Test
        @DisplayName("Bez primanja: stavka postoji s 0 bodova")
        void bezPrimanja() {
            Prijava p = prijava(3, "8.00", 10);
            p.setBrojClanovaDomacinstva(4);
            assertEquals(0, bodovi(p, "Primanja domaćinstva"), D);
        }

        @Test
        @DisplayName("Bez godine studija uspjeh je 0")
        void bezGodine() {
            Prijava p = new Prijava();
            p.setProsjek(new BigDecimal("8.00"));
            assertEquals(0, bodovi(p, "Uspjeh"), D);
        }

        @Test
        @DisplayName("Uvijek postoje stavke za uspjeh, udaljenost i primanja")
        void osnovneStavkeUvijek() {
            RezultatBodovanja r = servis.izracunaj(new Prijava());
            assertNotNull(stavka(r, "Uspjeh"));
            assertNotNull(stavka(r, "Udaljenost"));
            assertNotNull(stavka(r, "Primanja domaćinstva"));
        }

        @Test
        @DisplayName("Bodovi su konačni tek kad je prijava odobrena ili zaključena")
        void konacno() {
            assertFalse(saStatusom(Statusi.NA_PREGLEDU).konacno());
            assertFalse(saStatusom(Statusi.NEPOTPUNA).konacno());
            assertFalse(saStatusom(Statusi.NA_CEKANJU).konacno());
            assertTrue(saStatusom(Statusi.ODOBRENO).konacno());
            assertTrue(saStatusom(Statusi.ZAKLJUCENO).konacno());
        }

        private RezultatBodovanja saStatusom(String naziv) {
            Prijava p = prijava(3, "8.00", 10);
            var s = new ba.unze.edom.server.entity.StatusPrijave();
            s.setNaziv(naziv);
            p.setStatus(s);
            return servis.izracunaj(p);
        }
    }

    // =====================================================================
    // PRAVILA IZ POSTAVKI (admin ih može mijenjati)
    // =====================================================================

    @Nested
    @DisplayName("Bodovanje prati pravila iz Postavki")
    class PravilaIzPostavki {

        private final PravilaBodovanja def = podrazumijevanePostavke().pravila();

        /** Servis kojem Postavke vraćaju zadana pravila. */
        private BodovanjeService sa(PravilaBodovanja pravila) {
            PostavkeService postavke = org.mockito.Mockito.mock(PostavkeService.class);
            org.mockito.Mockito.when(postavke.pravila()).thenReturn(pravila);
            return new BodovanjeService(new PrijavaValidator(), postavke);
        }

        @Test
        @DisplayName("Promijenjen množilac za brucoše mijenja bodove")
        void brucosMnozilac() {
            var pr = new PravilaBodovanja(10, 0, def.prosjekMnozilac(), def.ispitMnozilac(),
                    def.bonusGodine(), def.kaznaPonovniUpis(), def.udaljenost(), def.primanja());
            RezultatBodovanja r = sa(pr).izracunaj(prijava(1, "4.50", null));
            assertEquals(45.0, stavka(r, "Uspjeh (brucoš)").bodovi(), D);
        }

        @Test
        @DisplayName("Kazna 0 za ponovni upis: stavka se ne pojavljuje")
        void bezKazne() {
            var pr = new PravilaBodovanja(def.brucosMnozilac(), def.brucosDodatak(), def.prosjekMnozilac(),
                    def.ispitMnozilac(), def.bonusGodine(), 0, def.udaljenost(), def.primanja());
            Prijava p = prijava(3, "8.50", 20);
            p.setUpisGodinePut(2);
            RezultatBodovanja r = sa(pr).izracunaj(p);
            assertNull(stavka(r, "Ponovni upis godine"));
            assertEquals(70.75, r.bodovi(), D);
        }

        @Test
        @DisplayName("Promijenjena skala udaljenosti mijenja bodove")
        void drugaSkalaUdaljenosti() {
            var skala = new PravilaBodovanja.Skala(
                    java.util.List.of(new PravilaBodovanja.Prag(false, 100, 0)), 30);
            var pr = new PravilaBodovanja(def.brucosMnozilac(), def.brucosDodatak(), def.prosjekMnozilac(),
                    def.ispitMnozilac(), def.bonusGodine(), def.kaznaPonovniUpis(), skala, def.primanja());

            Prijava blizu = prijava(3, "8.00", 10);
            blizu.setUdaljenostKm(new BigDecimal("95"));
            Prijava daleko = prijava(3, "8.00", 10);
            daleko.setUdaljenostKm(new BigDecimal("100"));

            assertEquals(0, stavka(sa(pr).izracunaj(blizu), "Udaljenost").bodovi(), D);
            assertEquals(30, stavka(sa(pr).izracunaj(daleko), "Udaljenost").bodovi(), D);
        }

        @Test
        @DisplayName("Godina bez zadanog bonusa dobija 0")
        void bonusNijeZadan() {
            var pr = new PravilaBodovanja(def.brucosMnozilac(), def.brucosDodatak(), def.prosjekMnozilac(),
                    def.ispitMnozilac(), java.util.Map.of(), def.kaznaPonovniUpis(), def.udaljenost(), def.primanja());
            // 8 × 3,5 = 28 + 10 × 1,8 = 18, bez bonusa
            assertEquals(46.0, stavka(sa(pr).izracunaj(prijava(3, "8.00", 10)), "Uspjeh").bodovi(), D);
        }
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

    private VrstaKriterija osRbih() {
        VrstaKriterija v = parametarski("DIJETE_OSRBIH");
        v.setNazivParametra("Broj punih mjeseci učešća");
        return v;
    }

    private void dodaj(Prijava p, VrstaKriterija v) {
        PrijavaKriterij k = new PrijavaKriterij();
        k.setVrsta(v);
        p.dodajKriterij(k);
    }

    private void dodajSParametrom(Prijava p, VrstaKriterija v, String parametar) {
        PrijavaKriterij k = new PrijavaKriterij();
        k.setVrsta(v);
        k.setParametar(new BigDecimal(parametar));
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