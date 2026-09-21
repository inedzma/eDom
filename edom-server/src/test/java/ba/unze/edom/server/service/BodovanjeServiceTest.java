package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BodovanjeServiceTest {

    private final BodovanjeService service = new BodovanjeService(new PrijavaValidator());

    // ---------- pomocne ----------

    private Prijava prazna() {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(1);
        // ovi testovi provjeravaju racun, pa se osnovni kriteriji
        // tretiraju kao vec provjereni
        p.setUspjehVerifikovan(true);
        p.setPrimanjaVerifikovana(true);
        p.setUdaljenostVerifikovana(true);
        return p;
    }

    private VrstaKriterija fiksni(String sifra, String naziv,
                                  double bodovi, String grupa) {
        VrstaKriterija v = new VrstaKriterija();
        v.setSifra(sifra);
        v.setNaziv(naziv);
        v.setBodovi(BigDecimal.valueOf(bodovi));
        v.setGrupa(grupa);
        v.setAktivan(true);
        return v;
    }

    private VrstaKriterija parametarski(String sifra, String naziv, String grupa) {
        VrstaKriterija v = new VrstaKriterija();
        v.setSifra(sifra);
        v.setNaziv(naziv);
        v.setTraziParametar(true);
        v.setNazivParametra("Parametar");
        v.setGrupa(grupa);
        v.setAktivan(true);
        return v;
    }

    private void dodaj(Prijava p, VrstaKriterija v,
                       Double parametar, boolean verifikovan) {
        PrijavaKriterij k = new PrijavaKriterij();
        k.setVrsta(v);
        if (parametar != null) k.setParametar(BigDecimal.valueOf(parametar));
        k.setVerifikovan(verifikovan);
        p.dodajKriterij(k);
    }

    // ================= PRAZNA PRIJAVA =================

    @Test
    @DisplayName("Prazna prijava ima 0 bodova i ne baca izuzetak")
    void praznaPrijava() {
        var r = service.izracunaj(prazna());
        assertEquals(0.0, r.potvrdjeno());
        assertEquals(0.0, r.potencijalno());
    }

    @Test
    @DisplayName("null prijava vraca nulu")
    void nullPrijava() {
        var r = service.izracunaj(null);
        assertEquals(0.0, r.potvrdjeno());
    }

    // ================= USPJEH =================

    @Test
    @DisplayName("Brucos: prosjek 4.5 daje 41.5 bodova")
    void brucos() {
        Prijava p = prazna();
        p.setGodinaStudija(1);
        p.setProsjek(new BigDecimal("4.50"));

        // 4.5 * 9 + 1 = 41.5
        assertEquals(41.5, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Brucosu se ne racunaju polozeni ispiti")
    void brucosBezIspita() {
        Prijava p = prazna();
        p.setGodinaStudija(1);
        p.setProsjek(new BigDecimal("4.00"));
        p.setPolozeniIspiti(30);          // ignorise se

        assertEquals(37.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Treca godina: prosjek 8.0, 12 ispita")
    void trecaGodina() {
        Prijava p = prazna();
        p.setGodinaStudija(3);
        p.setProsjek(new BigDecimal("8.00"));
        p.setPolozeniIspiti(12);

        // 8.0*3.5 + 12*1.8 + 5 = 28 + 21.6 + 5 = 54.6
        assertEquals(54.6, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Apsolvent dobija bonus 12")
    void apsolvent() {
        Prijava p = prazna();
        p.setGodinaStudija(GodineStudija.APSOLVENT);
        p.setProsjek(new BigDecimal("7.00"));
        p.setPolozeniIspiti(0);

        // 7.0*3.5 + 0 + 12 = 36.5
        assertEquals(36.5, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Postdiplomac dobija bonus 10")
    void postdiplomac() {
        Prijava p = prazna();
        p.setGodinaStudija(GodineStudija.POSTDIPLOMAC);
        p.setProsjek(new BigDecimal("9.00"));
        p.setPolozeniIspiti(0);

        // 9.0*3.5 + 10 = 41.5
        assertEquals(41.5, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Bez unesenog prosjeka nema bodova za uspjeh")
    void bezProsjeka() {
        Prijava p = prazna();
        p.setGodinaStudija(3);
        p.setPolozeniIspiti(10);

        assertEquals(0.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Bodovi za uspjeh nisu ograniceni odozgo")
    void uspjehNijeOgranicen() {
        Prijava p = prazna();
        p.setGodinaStudija(5);
        p.setProsjek(new BigDecimal("10.00"));
        p.setPolozeniIspiti(45);

        // 35 + 81 + 9 = 125 - starije godine namjerno imaju prednost
        assertEquals(125.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    // ================= UDALJENOST =================

    @Test
    @DisplayName("Granice udaljenosti")
    void udaljenost() {
        assertEquals(0.0,  bodoviZaUdaljenost(49.9), 0.001);
        assertEquals(4.0,  bodoviZaUdaljenost(50), 0.001);
        assertEquals(4.0,  bodoviZaUdaljenost(79.9), 0.001);
        assertEquals(8.0,  bodoviZaUdaljenost(80), 0.001);
        assertEquals(12.0, bodoviZaUdaljenost(120), 0.001);
        assertEquals(15.0, bodoviZaUdaljenost(160), 0.001);
        assertEquals(15.0, bodoviZaUdaljenost(500), 0.001);
    }

    private double bodoviZaUdaljenost(double km) {
        Prijava p = prazna();
        p.setUdaljenostKm(BigDecimal.valueOf(km));
        return service.izracunaj(p).potvrdjeno();
    }

    // ================= PRIMANJA =================

    @Test
    @DisplayName("Granice primanja po clanu domacinstva")
    void primanja() {
        assertEquals(20.0, bodoviZaPrimanja(400, 4), 0.001);   // 100 po clanu
        assertEquals(15.0, bodoviZaPrimanja(600, 4), 0.001);   // 150
        assertEquals(8.0,  bodoviZaPrimanja(800, 4), 0.001);   // 200
        assertEquals(4.0,  bodoviZaPrimanja(1200, 4), 0.001);  // 300
        assertEquals(2.0,  bodoviZaPrimanja(1600, 4), 0.001);  // 400
        assertEquals(0.0,  bodoviZaPrimanja(2000, 4), 0.001);  // 500
    }

    @Test
    @DisplayName("Domacinstvo od jednog clana - sam student")
    void jedanClan() {
        assertEquals(20.0, bodoviZaPrimanja(100, 1), 0.001);
    }

    @Test
    @DisplayName("Nula clanova je nemoguca - servis ne smije puci")
    void nulaClanova() {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(0);
        p.setUkupnaPrimanja(new BigDecimal("1000"));

        assertDoesNotThrow(() -> service.izracunaj(p));
        assertEquals(0.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    private double bodoviZaPrimanja(double ukupno, int clanova) {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(clanova);
        p.setUkupnaPrimanja(BigDecimal.valueOf(ukupno));
        return service.izracunaj(p).potvrdjeno();
    }

    // ================= BRANIOCI: uzima se najveci =================

    @Test
    @DisplayName("Od vise kriterija iz grupe BRANIOCI racuna se samo najveci")
    void braniociNajveci() {
        Prijava p = prazna();
        dodaj(p, fiksni("BEZ_JEDNOG_RODIT", "Bez jednog roditelja",
                15, GrupeKriterija.BRANIOCI), null, true);
        dodaj(p, fiksni("POGINULI_BRANILAC", "Dijete šehida",
                50, GrupeKriterija.BRANIOCI), null, true);
        dodaj(p, fiksni("INVALIDNOST", "Invalidnost",
                10, GrupeKriterija.BRANIOCI), null, true);

        // ne 75, nego 50
        assertEquals(50.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Dva jednaka kriterija daju bodove samo jednom")
    void braniociJednaki() {
        Prijava p = prazna();
        dodaj(p, fiksni("BEZ_OBA_RODITELJA", "Bez oba roditelja",
                20, GrupeKriterija.BRANIOCI), null, true);
        dodaj(p, fiksni("CLAN_POR_SEHIDA", "Član porodice šehida",
                20, GrupeKriterija.BRANIOCI), null, true);

        assertEquals(20.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Student RVI ispod 60 posto daje 15 bodova")
    void rviIspod60() {
        Prijava p = prazna();
        dodaj(p, parametarski("STUDENT_RVI", "Student RVI",
                GrupeKriterija.BRANIOCI), 40.0, true);

        assertEquals(15.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Student RVI od 60 posto i vise daje 20 bodova")
    void rviIznad60() {
        Prijava p = prazna();
        dodaj(p, parametarski("STUDENT_RVI", "Student RVI",
                GrupeKriterija.BRANIOCI), 60.0, true);

        assertEquals(20.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Invalidnost roditelja: postotak puta 0.1")
    void invalidnostRoditelja() {
        Prijava p = prazna();
        dodaj(p, parametarski("INVALID_RODITELJA", "Invalidnost roditelja",
                GrupeKriterija.BRANIOCI), 80.0, true);

        assertEquals(8.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Dijete OSRBiH: mjeseci puta 0.3")
    void dijeteOsrbih() {
        Prijava p = prazna();
        dodaj(p, parametarski("DIJETE_OSRBIH", "Dijete učesnika OS RBiH",
                GrupeKriterija.BRANIOCI), 36.0, true);

        assertEquals(10.8, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Parametarski kriterij bez parametra daje 0")
    void parametarskiBezParametra() {
        Prijava p = prazna();
        dodaj(p, parametarski("STUDENT_RVI", "Student RVI",
                GrupeKriterija.BRANIOCI), null, true);

        assertEquals(0.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    // ================= DODATNI: zbrajaju se =================

    @Test
    @DisplayName("Kriteriji iz grupe DODATNI se zbrajaju")
    void dodatniSeZbrajaju() {
        Prijava p = prazna();
        dodaj(p, fiksni("IZBJEGLICA", "Izbjeglica",
                3, GrupeKriterija.DODATNI), null, true);
        dodaj(p, fiksni("BRAT_SESTRA", "Brat/sestra studiraju",
                2, GrupeKriterija.DODATNI), null, true);

        assertEquals(5.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    @Test
    @DisplayName("Isti kriterij se ne moze bodovati dvaput")
    void nemaDuplogBodovanja() {
        Prijava p = prazna();
        var bratSestra = fiksni("BRAT_SESTRA", "Brat/sestra studiraju",
                2, GrupeKriterija.DODATNI);

        dodaj(p, bratSestra, null, true);
        dodaj(p, bratSestra, null, true);   // baza bi ovo odbila (UNIQUE)

        // servis svejedno broji jednom
        assertEquals(2.0, service.izracunaj(p).potvrdjeno(), 0.001);
    }

    // ================= VERIFIKACIJA =================

    @Test
    @DisplayName("Neverifikovan kriterij ne ulazi u potvrdjene bodove")
    void neverifikovanNeUlazi() {
        Prijava p = prazna();
        dodaj(p, fiksni("POGINULI_BRANILAC", "Dijete šehida",
                50, GrupeKriterija.BRANIOCI), null, false);

        var r = service.izracunaj(p);
        assertEquals(0.0, r.potvrdjeno(), 0.001);
        assertEquals(50.0, r.potencijalno(), 0.001);
        assertTrue(r.cekaVerifikaciju());
    }

    @Test
    @DisplayName("Neverifikovan uspjeh ne ulazi u potvrdjene bodove")
    void neverifikovanUspjeh() {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(1);
        p.setGodinaStudija(1);
        p.setProsjek(new BigDecimal("4.00"));
        // uspjehVerifikovan ostaje false

        var r = service.izracunaj(p);
        assertEquals(0.0, r.potvrdjeno(), 0.001);
        assertEquals(37.0, r.potencijalno(), 0.001);
        assertTrue(r.cekaVerifikaciju());
    }

    @Test
    @DisplayName("Verifikacija uspjeha ne priznaje primanja")
    void verifikacijaJePoStavci() {
        Prijava p = new Prijava();
        p.setBrojClanovaDomacinstva(4);
        p.setGodinaStudija(1);
        p.setProsjek(new BigDecimal("4.00"));       // 37
        p.setUkupnaPrimanja(new BigDecimal("400")); // 20
        p.setUspjehVerifikovan(true);
        // primanjaVerifikovana ostaje false

        var r = service.izracunaj(p);
        assertEquals(37.0, r.potvrdjeno(), 0.001);
        assertEquals(57.0, r.potencijalno(), 0.001);
    }

    @Test
    @DisplayName("Bodovi su konacni tek kad je prijava odobrena")
    void konacnoTekNakonOdobrenja() {
        Prijava p = prazna();
        p.setGodinaStudija(1);
        p.setProsjek(new BigDecimal("4.00"));

        assertFalse(service.izracunaj(p).konacno());

        StatusPrijave odobreno = new StatusPrijave();
        odobreno.setNaziv(Statusi.ODOBRENO);
        p.setStatus(odobreno);

        assertTrue(service.izracunaj(p).konacno());
    }

    @Test
    @DisplayName("Verifikovan i neverifikovan kriterij zajedno")
    void mjesovito() {
        Prijava p = prazna();
        dodaj(p, fiksni("IZBJEGLICA", "Izbjeglica",
                3, GrupeKriterija.DODATNI), null, true);
        dodaj(p, fiksni("BRAT_SESTRA", "Brat/sestra",
                2, GrupeKriterija.DODATNI), null, false);

        var r = service.izracunaj(p);
        assertEquals(3.0, r.potvrdjeno(), 0.001);
        assertEquals(5.0, r.potencijalno(), 0.001);
    }

    @Test
    @DisplayName("Najveci branilac se bira odvojeno za potvrdjeno i potencijalno")
    void najveciOdvojeno() {
        Prijava p = prazna();
        dodaj(p, fiksni("POGINULI_BRANILAC", "Dijete šehida",
                50, GrupeKriterija.BRANIOCI), null, false);   // ceka
        dodaj(p, fiksni("INVALIDNOST", "Invalidnost",
                10, GrupeKriterija.BRANIOCI), null, true);    // potvrdjeno

        var r = service.izracunaj(p);
        assertEquals(10.0, r.potvrdjeno(), 0.001);
        assertEquals(50.0, r.potencijalno(), 0.001);
    }

    // ================= RAZLOZENI PRIKAZ =================

    @Test
    @DisplayName("Rezultat sadrzi stavke sa obrazlozenjem")
    void stavkeSuRazlozene() {
        Prijava p = prazna();
        p.setGodinaStudija(2);
        p.setProsjek(new BigDecimal("8.00"));
        p.setPolozeniIspiti(10);
        p.setUdaljenostKm(new BigDecimal("100"));

        var r = service.izracunaj(p);

        assertFalse(r.stavke().isEmpty());
        assertTrue(r.stavke().stream()
                .anyMatch(s -> s.naziv().contains("Uspjeh")));
        assertTrue(r.stavke().stream()
                .anyMatch(s -> s.naziv().contains("Udaljenost")));
    }

    // ================= POTPUNA PRIJAVA =================

    @Test
    @DisplayName("Potpuna prijava: svi kriteriji zajedno")
    void potpunaPrijava() {
        Prijava p = new Prijava();
        p.setGodinaStudija(3);
        p.setProsjek(new BigDecimal("8.00"));
        p.setPolozeniIspiti(12);                          // 54.6
        p.setUdaljenostKm(new BigDecimal("130"));         // 12
        p.setBrojClanovaDomacinstva(5);
        p.setUkupnaPrimanja(new BigDecimal("500"));       // 100/clan -> 20

        dodaj(p, fiksni("BEZ_JEDNOG_RODIT", "Bez jednog roditelja",
                15, GrupeKriterija.BRANIOCI), null, true);   // 15
        dodaj(p, fiksni("IZBJEGLICA", "Izbjeglica",
                3, GrupeKriterija.DODATNI), null, true);     // 3

        // 54.6 + 12 + 20 + 15 + 3 = 104.6
        assertEquals(104.6, service.izracunaj(p).potvrdjeno(), 0.001);
    }
}