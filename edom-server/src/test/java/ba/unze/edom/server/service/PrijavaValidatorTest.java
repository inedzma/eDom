package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PrijavaValidatorTest {

    private final PrijavaValidator validator = new PrijavaValidator();

    private Prijava prijava() {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(4);
        return p;
    }

    // ================= NULL JE DOZVOLJEN =================

    @Test
    @DisplayName("Prazna prijava prolazi validaciju")
    void praznaProlazi() {
        assertDoesNotThrow(() -> validator.provjeri(new Prijava()));
    }

    @Test
    @DisplayName("null prijava prolazi")
    void nullProlazi() {
        assertDoesNotThrow(() -> validator.provjeri(null));
    }

    // ================= GODINA STUDIJA =================

    @Test
    @DisplayName("Godina studija 0 se odbija")
    void godinaNula() {
        Prijava p = prijava();
        p.setGodinaStudija(0);
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Godina studija 9 se odbija")
    void godinaPrevelika() {
        Prijava p = prijava();
        p.setGodinaStudija(9);
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Negativna godina studija se odbija")
    void godinaNegativna() {
        Prijava p = prijava();
        p.setGodinaStudija(-1);
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Sve validne godine prolaze")
    void validneGodine() {
        for (int g = 1; g <= 8; g++) {
            Prijava p = prijava();
            p.setGodinaStudija(g);
            int finalG = g;
            assertDoesNotThrow(() -> validator.provjeri(p),
                    "Godina " + finalG + " bi trebala biti validna");
        }
    }

    // ================= PROSJEK =================

    @Test
    @DisplayName("Negativan prosjek se odbija")
    void prosjekNegativan() {
        Prijava p = prijava();
        p.setGodinaStudija(3);
        p.setProsjek(new BigDecimal("-3.00"));
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Brucos sa prosjekom 9.5 se odbija - pogresna skala")
    void brucosFakultetskaSkala() {
        Prijava p = prijava();
        p.setGodinaStudija(1);
        p.setProsjek(new BigDecimal("9.50"));

        var greska = assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
        assertTrue(greska.getMessage().contains("srednje škole"));
    }

    @Test
    @DisplayName("Treca godina sa prosjekom 4.0 se odbija - pogresna skala")
    void starijaSrednjoskolskaSkala() {
        Prijava p = prijava();
        p.setGodinaStudija(3);
        p.setProsjek(new BigDecimal("4.00"));
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Prosjek 10.5 se odbija")
    void prosjekIznadDeset() {
        Prijava p = prijava();
        p.setGodinaStudija(4);
        p.setProsjek(new BigDecimal("10.50"));
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Granicne vrijednosti prosjeka prolaze")
    void granicniProsjeci() {
        Prijava brucos = prijava();
        brucos.setGodinaStudija(1);
        brucos.setProsjek(new BigDecimal("1.00"));
        assertDoesNotThrow(() -> validator.provjeri(brucos));

        brucos.setProsjek(new BigDecimal("5.00"));
        assertDoesNotThrow(() -> validator.provjeri(brucos));

        Prijava starija = prijava();
        starija.setGodinaStudija(3);
        starija.setProsjek(new BigDecimal("6.00"));
        assertDoesNotThrow(() -> validator.provjeri(starija));

        starija.setProsjek(new BigDecimal("10.00"));
        assertDoesNotThrow(() -> validator.provjeri(starija));
    }

    @Test
    @DisplayName("Prosjek bez godine studija se ne provjerava po skali")
    void prosjekBezGodine() {
        Prijava p = prijava();
        p.setProsjek(new BigDecimal("7.50"));
        assertDoesNotThrow(() -> validator.provjeri(p));
    }

    // ================= ISPITI =================

    @Test
    @DisplayName("Negativan broj ispita se odbija")
    void ispitiNegativni() {
        Prijava p = prijava();
        p.setPolozeniIspiti(-5);
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Nerealno velik broj ispita se odbija")
    void ispitiPreviseVelik() {
        Prijava p = prijava();
        p.setPolozeniIspiti(500);
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Nula ispita je dozvoljena")
    void nulaIspita() {
        Prijava p = prijava();
        p.setPolozeniIspiti(0);
        assertDoesNotThrow(() -> validator.provjeri(p));
    }

    // ================= UDALJENOST =================

    @Test
    @DisplayName("Negativna udaljenost se odbija")
    void udaljenostNegativna() {
        Prijava p = prijava();
        p.setUdaljenostKm(new BigDecimal("-50"));
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Nerealna udaljenost se odbija")
    void udaljenostPrevelika() {
        Prijava p = prijava();
        p.setUdaljenostKm(new BigDecimal("5000"));
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Udaljenost 0 je dozvoljena - student iz Zenice")
    void udaljenostNula() {
        Prijava p = prijava();
        p.setUdaljenostKm(BigDecimal.ZERO);
        assertDoesNotThrow(() -> validator.provjeri(p));
    }

    // ================= DOMACINSTVO =================

    @Test
    @DisplayName("Nula clanova domacinstva se odbija")
    void nulaClanova() {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(0);

        var greska = assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
        assertTrue(greska.getMessage().contains("najmanje jednog"));
    }

    @Test
    @DisplayName("Negativan broj clanova se odbija")
    void negativnoClanova() {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(-3);
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Jedan clan je dozvoljen - sam student")
    void jedanClan() {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(1);
        assertDoesNotThrow(() -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Nerealan broj clanova se odbija")
    void previseClanova() {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(100);
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Negativna primanja se odbijaju")
    void primanjaNegativna() {
        Prijava p = prijava();
        p.setUkupnaPrimanja(new BigDecimal("-200"));
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Primanja bez broja clanova se odbijaju")
    void primanjaBezClanova() {
        Prijava p = new Prijava();
        p.setUkupnaPrimanja(new BigDecimal("800"));

        var greska = assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
        assertTrue(greska.getMessage().contains("broj članova"));
    }

    @Test
    @DisplayName("Primanja 0 su dozvoljena")
    void primanjaNula() {
        Prijava p = prijava();
        p.setUkupnaPrimanja(BigDecimal.ZERO);
        assertDoesNotThrow(() -> validator.provjeri(p));
    }

    // ================= KRITERIJI =================

    private VrstaKriterija rvi() {
        VrstaKriterija v = new VrstaKriterija();
        v.setSifra("STUDENT_RVI");
        v.setNaziv("Student RVI");
        v.setTraziParametar(true);
        v.setGrupa("BRANIOCI");
        return v;
    }

    private void dodajKriterij(Prijava p, VrstaKriterija v, Double param) {
        PrijavaKriterij k = new PrijavaKriterij();
        k.setVrsta(v);
        if (param != null) k.setParametar(BigDecimal.valueOf(param));
        p.dodajKriterij(k);
    }

    @Test
    @DisplayName("Postotak invalidnosti preko 100 se odbija")
    void postotakPreko100() {
        Prijava p = prijava();
        dodajKriterij(p, rvi(), 150.0);

        var greska = assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
        assertTrue(greska.getMessage().contains("100"));
    }

    @Test
    @DisplayName("Negativan parametar se odbija")
    void parametarNegativan() {
        Prijava p = prijava();
        dodajKriterij(p, rvi(), -20.0);
        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Postotak 100 je dozvoljen")
    void postotak100() {
        Prijava p = prijava();
        dodajKriterij(p, rvi(), 100.0);
        assertDoesNotThrow(() -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Parametarski kriterij bez parametra prolazi")
    void bezParametra() {
        Prijava p = prijava();
        dodajKriterij(p, rvi(), null);
        assertDoesNotThrow(() -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Kriterij bez vrste se odbija")
    void kriterijBezVrste() {
        Prijava p = prijava();
        PrijavaKriterij k = new PrijavaKriterij();
        p.dodajKriterij(k);

        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }

    @Test
    @DisplayName("Nerealan broj mjeseci OSRBiH se odbija")
    void previseMjeseci() {
        VrstaKriterija v = new VrstaKriterija();
        v.setSifra("DIJETE_OSRBIH");
        v.setNaziv("Dijete OS RBiH");
        v.setTraziParametar(true);
        v.setGrupa("BRANIOCI");

        Prijava p = prijava();
        dodajKriterij(p, v, 9999.0);

        assertThrows(NevalidnaPrijavaException.class,
                () -> validator.provjeri(p));
    }
}