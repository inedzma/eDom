package ba.unze.edom.server.service.bodovanje;

import ba.unze.edom.common.dto.bodovanje.BodovanjeDetaljnoDTO;
import ba.unze.edom.common.dto.bodovanje.BodovanjeIzmjenaDTO;
import ba.unze.edom.common.dto.bodovanje.BodovanjeKriterijIzmjenaDTO;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.entity.VrstaKriterija;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.VrstaKriterijaRepository;
import ba.unze.edom.server.service.prijava.PrijavaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Admin na desktopu ispravlja podatke prijave i kriterije nakon pregleda dokumenata
 * (npr. uklanja kriterij za koji student nije donio dokaz), a bodovi se odmah preračunaju.
 *
 * Repozitoriji su Mockito objekti; bodovanje je pravo (BodovanjeService s pravilima iz Pravilnika).
 */
@ExtendWith(MockitoExtension.class)
class BodovanjeAdminServiceTest {

    private static final double D = 0.001;
    private static final int ID = 15;

    // vrste kriterija kakve su u bazi
    private static final VrstaKriterija BEZ_JEDNOG = fiksni(1, "BEZ_JEDNOG_RODIT", 15, "BRANIOCI");
    private static final VrstaKriterija IZBJEGLICA = fiksni(2, "IZBJEGLICA", 3, "DODATNI");
    private static final VrstaKriterija RVI = parametarski(3, "STUDENT_RVI", "Postotak invalidnosti");

    @Mock PrijavaRepository prijavaRepository;
    @Mock VrstaKriterijaRepository vrstaKriterijaRepository;

    private BodovanjeAdminService servis;
    private Prijava prijava;

    @BeforeEach
    void pripremi() {
        servis = new BodovanjeAdminService(prijavaRepository, vrstaKriterijaRepository,
                new BodovanjeService(new PrijavaValidator(), BodovanjeServiceTest.podrazumijevanePostavke()));

        prijava = new Prijava();
        prijava.setIdPrijava(ID);
        prijava.setGodinaStudija(3);
        prijava.setProsjek(new BigDecimal("8.00"));
        prijava.setPolozeniIspiti(10);                // uspjeh 51
        prijava.setUkupniBodovi(50.0);
    }

    private void postoji() {
        when(prijavaRepository.zaDetalje(ID)).thenReturn(Optional.of(prijava));
    }

    // =====================================================================
    // PODACI PRIJAVE
    // =====================================================================

    @Test
    @DisplayName("Izmjena prosjeka: bodovi se odmah preračunaju i snime u prijavu")
    void preracunavanje() {
        postoji();

        BodovanjeDetaljnoDTO r = servis.sacuvaj(ID, izmjena(3, 1, "9.00", 20, List.of()));

        // 9 × 3,5 = 31,5 + 20 × 1,8 = 36 + bonus 5
        assertEquals(72.5, r.bodovi(), D);
        assertEquals(72.5, prijava.getUkupniBodovi(), D);
        assertEquals(0, new BigDecimal("9.00").compareTo(prijava.getProsjek()));
    }

    @Test
    @DisplayName("Bez broja upisa podrazumijeva se prvi upis")
    void upisPodrazumijevano() {
        postoji();
        servis.sacuvaj(ID, izmjena(3, null, "8.00", 10, List.of()));
        assertEquals(1, prijava.getUpisGodinePut());
    }

    @Test
    @DisplayName("Ponovni upis koji admin utvrdi iz dokumenta smanjuje bodove za 3")
    void ponovniUpis() {
        postoji();
        assertEquals(48.0, servis.sacuvaj(ID, izmjena(3, 2, "8.00", 10, List.of())).bodovi(), D);
    }

    @Test
    @DisplayName("Neispravni podaci se ne snimaju: greška, a bodovi ostaju stari")
    void neispravniPodaci() {
        postoji();

        var g = assertThrows(PrijavaException.class,
                () -> servis.sacuvaj(ID, izmjena(3, 1, "11.00", 10, List.of())));

        assertTrue(g.getMessage().contains("6.00 i 10.00"));
        assertEquals(50.0, prijava.getUkupniBodovi(), D);
    }

    @Test
    @DisplayName("Nepostojeća prijava")
    void nepostojeca() {
        when(prijavaRepository.zaDetalje(99)).thenReturn(Optional.empty());
        var g = assertThrows(PrijavaException.class, () -> servis.detaljno(99));
        assertEquals("Prijava ne postoji.", g.getMessage());
    }

    // =====================================================================
    // KRITERIJI
    // =====================================================================

    @Test
    @DisplayName("Kriterij bez dokaza se uklanja i ne boduje se")
    void uklanjanjeKriterija() {
        dodaj(BEZ_JEDNOG, null);
        dodaj(IZBJEGLICA, null);
        postoji();

        BodovanjeDetaljnoDTO r = servis.sacuvaj(ID,
                izmjena(3, 1, "8.00", 10, List.of(kr(IZBJEGLICA, null))));

        assertEquals(1, prijava.getKriteriji().size());
        assertEquals(51 + 3, r.bodovi(), D);
    }

    @Test
    @DisplayName("Uklanjanjem svih kriterija ostaje samo osnovno bodovanje")
    void uklanjanjeSvih() {
        dodaj(BEZ_JEDNOG, null);
        postoji();

        assertEquals(51, servis.sacuvaj(ID, izmjena(3, 1, "8.00", 10, List.of())).bodovi(), D);
        assertTrue(prijava.getKriteriji().isEmpty());
    }

    @Test
    @DisplayName("Admin dodaje kriterij koji je student zaboravio označiti")
    void dodavanjeKriterija() {
        postoji();
        when(vrstaKriterijaRepository.findById(3)).thenReturn(Optional.of(RVI));

        BodovanjeDetaljnoDTO r = servis.sacuvaj(ID,
                izmjena(3, 1, "8.00", 10, List.of(kr(RVI, "60"))));

        assertEquals(51 + 20, r.bodovi(), D);
        assertEquals(0, new BigDecimal("60").compareTo(prijava.getKriteriji().get(0).getParametar()));
    }

    @Test
    @DisplayName("Ispravka parametra postojećeg kriterija (RVI 30% → 60%)")
    void izmjenaParametra() {
        dodaj(RVI, "30");
        postoji();

        assertEquals(51 + 15, servis.detaljno(ID).bodovi(), D);
        assertEquals(51 + 20, servis.sacuvaj(ID,
                izmjena(3, 1, "8.00", 10, List.of(kr(RVI, "60")))).bodovi(), D);
        assertEquals(1, prijava.getKriteriji().size());
    }

    @Test
    @DisplayName("Parametarski kriterij bez parametra se odbija")
    void bezParametra() {
        dodaj(RVI, "30");
        postoji();

        var g = assertThrows(PrijavaException.class, () -> servis.sacuvaj(ID,
                izmjena(3, 1, "8.00", 10, List.of(kr(RVI, null)))));
        assertEquals("Za kriterij 'STUDENT_RVI' unesite postotak invalidnosti.", g.getMessage());
    }

    @Test
    @DisplayName("Kod kriterija s fiksnim bodovima parametar se ne snima")
    void fiksniBezParametra() {
        dodaj(BEZ_JEDNOG, null);
        postoji();

        servis.sacuvaj(ID, izmjena(3, 1, "8.00", 10, List.of(kr(BEZ_JEDNOG, "99"))));
        assertNull(prijava.getKriteriji().get(0).getParametar());
    }

    @Test
    @DisplayName("Isti kriterij dvaput u zahtjevu se dodaje samo jednom")
    void duplikat() {
        postoji();
        when(vrstaKriterijaRepository.findById(2)).thenReturn(Optional.of(IZBJEGLICA));

        servis.sacuvaj(ID, izmjena(3, 1, "8.00", 10, List.of(kr(IZBJEGLICA, null), kr(IZBJEGLICA, null))));
        assertEquals(1, prijava.getKriteriji().size());
    }

    @Test
    @DisplayName("Nepoznat kriterij se odbija")
    void nepoznatKriterij() {
        postoji();
        when(vrstaKriterijaRepository.findById(77)).thenReturn(Optional.empty());

        var g = assertThrows(PrijavaException.class, () -> servis.sacuvaj(ID,
                izmjena(3, 1, "8.00", 10, List.of(new BodovanjeKriterijIzmjenaDTO(77, null)))));
        assertEquals("Nepoznat kriterij.", g.getMessage());
    }

    // =====================================================================
    // PRIKAZ
    // =====================================================================

    @Test
    @DisplayName("Prikaz prijave s neispravnim podacima ne puca: greška se prikazuje adminu")
    void prikazNeispravne() {
        prijava.setProsjek(new BigDecimal("4.00"));   // 3. godina, srednjoškolski prosjek
        postoji();

        BodovanjeDetaljnoDTO r = servis.detaljno(ID);

        assertNotNull(r.greska());
        assertEquals(0, r.bodovi(), D);
    }

    @Test
    @DisplayName("Prikaz: svi aktivni kriteriji, označeni i neoznačeni")
    void sviAktivniKriteriji() {
        dodaj(BEZ_JEDNOG, null);
        postoji();
        when(vrstaKriterijaRepository.findByAktivanTrueOrderByGrupaAscNazivAsc())
                .thenReturn(List.of(BEZ_JEDNOG, IZBJEGLICA, RVI));

        BodovanjeDetaljnoDTO r = servis.detaljno(ID);

        assertEquals(3, r.kriteriji().size());
        assertNull(r.greska());
        assertEquals(51 + 15, r.bodovi(), D);
    }

    @Test
    @DisplayName("Prikaz: označen kriterij koji više nije aktivan se i dalje vidi")
    void neaktivanOznacen() {
        VrstaKriterija stari = fiksni(9, "STARI_KRITERIJ", 5, "DODATNI");
        dodaj(stari, null);
        postoji();
        when(vrstaKriterijaRepository.findByAktivanTrueOrderByGrupaAscNazivAsc())
                .thenReturn(List.of(BEZ_JEDNOG));

        assertEquals(2, servis.detaljno(ID).kriteriji().size());
    }

    // ---------- pomoćne ----------

    /**
     * Izmjena kakvu šalje desktop.
     * Redoslijed polja: godina, upis, prosjek, ispiti, udaljenost, članovi, primanja, kriteriji.
     * Ako se BodovanjeIzmjenaDTO crveni, uskladi samo ovu metodu.
     */
    private static BodovanjeIzmjenaDTO izmjena(Integer godina, Integer upis, String prosjek, Integer ispiti,
                                               List<BodovanjeKriterijIzmjenaDTO> kriteriji) {
        return new BodovanjeIzmjenaDTO(godina, upis, new BigDecimal(prosjek), ispiti,
                null, null, null, kriteriji);
    }

    private static BodovanjeKriterijIzmjenaDTO kr(VrstaKriterija v, String parametar) {
        return new BodovanjeKriterijIzmjenaDTO(v.getIdVrstaKriterija(),
                parametar != null ? new BigDecimal(parametar) : null);
    }

    private void dodaj(VrstaKriterija v, String parametar) {
        PrijavaKriterij k = new PrijavaKriterij();
        k.setVrsta(v);
        if (parametar != null) k.setParametar(new BigDecimal(parametar));
        prijava.dodajKriterij(k);
    }

    private static VrstaKriterija fiksni(int id, String sifra, double bodovi, String grupa) {
        VrstaKriterija v = new VrstaKriterija();
        v.setIdVrstaKriterija(id);
        v.setSifra(sifra);
        v.setNaziv(sifra);
        v.setBodovi(BigDecimal.valueOf(bodovi));
        v.setGrupa(grupa);
        return v;
    }

    private static VrstaKriterija parametarski(int id, String sifra, String nazivParametra) {
        VrstaKriterija v = new VrstaKriterija();
        v.setIdVrstaKriterija(id);
        v.setSifra(sifra);
        v.setNaziv(sifra);
        v.setGrupa("BRANIOCI");
        v.setTraziParametar(true);
        v.setNazivParametra(nazivParametra);
        return v;
    }
}