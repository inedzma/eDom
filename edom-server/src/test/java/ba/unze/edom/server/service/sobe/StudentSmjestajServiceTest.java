package ba.unze.edom.server.service.sobe;

import ba.unze.edom.server.dto.MojSmjestaj;
import ba.unze.edom.server.dto.ZeljaForma;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.SmjestajRepository;
import ba.unze.edom.server.repository.SobaRepository;
import ba.unze.edom.server.repository.ZeljaSmjestajaRepository;
import ba.unze.edom.server.service.prijava.Statusi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Želja za smještaj (prije dodjele sobe) i zahtjev za promjenu sobe (poslije dodjele).
 * Ista forma, ista tabela; razlika je samo što poslije dodjele razlog u napomeni postaje obavezan.
 *
 * Repozitoriji su Mockito objekti, pa baza nije potrebna.
 */
@ExtendWith(MockitoExtension.class)
class StudentSmjestajServiceTest {

    private static final int ID_STUDENTA = 5;
    private static final int ID_PRIJAVE = 100;
    private static final int GODINA = 2026;

    @Mock SmjestajRepository smjestajRepository;
    @Mock ZeljaSmjestajaRepository zeljaRepository;
    @Mock SobaRepository sobaRepository;

    private StudentSmjestajService servis;
    private Prijava prijava;

    @BeforeEach
    void setUp() {
        servis = new StudentSmjestajService(smjestajRepository, zeljaRepository, sobaRepository);
        prijava = new Prijava();
        prijava.setIdPrijava(ID_PRIJAVE);
        prijava.setAkademskaGodina(GODINA);
    }

    // =====================================================================
    // KO SMIJE UNIJETI ŽELJU
    // =====================================================================

    @Nested
    @DisplayName("Ko smije unijeti želju")
    class Pravo {

        @Test
        @DisplayName("Student bez prijave ne može unijeti želju")
        void bezPrijave() {
            when(smjestajRepository.prijaveStudenta(ID_STUDENTA)).thenReturn(List.of());

            var g = assertThrows(PrijavaException.class, () -> servis.sacuvajZelju(ID_STUDENTA, forma()));
            assertEquals("Nemate prijavu.", g.getMessage());
            verify(zeljaRepository, never()).save(any());
        }

        @ParameterizedTest(name = "status \"{0}\": želja se odbija")
        @ValueSource(strings = {Statusi.U_IZRADI, Statusi.NA_PREGLEDU, Statusi.NEPOTPUNA, Statusi.ODBIJENO})
        @DisplayName("Prijava koja nije odobrena: želja se odbija")
        void nijeOdobrena(String status) {
            assumeFalse(SobeService.STATUSI_ZA_RASPORED.contains(status));
            imaPrijavu(status);

            var g = assertThrows(PrijavaException.class, () -> servis.sacuvajZelju(ID_STUDENTA, forma()));
            assertEquals("Želje se mogu unijeti tek kada je prijava odobrena.", g.getMessage());
            verify(zeljaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Svaki status koji ulazi u raspored dozvoljava želju")
        void odobrena() {
            for (String status : SobeService.STATUSI_ZA_RASPORED) {
                imaPrijavu(status);
                assertDoesNotThrow(() -> servis.sacuvajZelju(ID_STUDENTA, forma()), status);
            }
        }

        @Test
        @DisplayName("Gleda se posljednja prijava (prva koju repozitorij vrati)")
        void posljednjaPrijava() {
            Prijava stara = new Prijava();
            stara.setIdPrijava(1);
            stara.setStatus(status(Statusi.ODBIJENO));
            prijava.setStatus(status(Statusi.ODOBRENO));
            when(smjestajRepository.prijaveStudenta(ID_STUDENTA)).thenReturn(List.of(prijava, stara));

            servis.sacuvajZelju(ID_STUDENTA, forma());

            assertSame(prijava, sacuvana().getPrijava());
        }
    }

    // =====================================================================
    // PRIJE DODJELE SOBE I POSLIJE (ZAHTJEV ZA PROMJENU)
    // =====================================================================

    @Nested
    @DisplayName("Prije i poslije dodjele sobe")
    class DodjelaSobe {

        @Test
        @DisplayName("Prije dodjele napomena nije obavezna")
        void prijeDodjele() {
            imaPrijavu(Statusi.ODOBRENO);

            servis.sacuvajZelju(ID_STUDENTA, forma());

            assertNull(sacuvana().getNapomena());
        }

        @ParameterizedTest(name = "smještaj \"{0}\": bez razloga se odbija")
        @ValueSource(strings = {Smjestaj.DODIJELJEN, Smjestaj.USELJEN})
        @DisplayName("Soba dodijeljena ili useljen: zahtjev bez razloga se odbija")
        void poslijeDodjeleBezRazloga(String statusSmjestaja) {
            imaPrijavu(Statusi.ODOBRENO);
            imaSmjestaj(statusSmjestaja);

            var g = assertThrows(PrijavaException.class, () -> servis.sacuvajZelju(ID_STUDENTA, forma()));
            assertEquals("Soba vam je već dodijeljena. U napomenu upišite razlog promjene sobe.", g.getMessage());
            verify(zeljaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Razlog od samih razmaka se ne računa")
        void razlogRazmaci() {
            imaPrijavu(Statusi.ODOBRENO);
            imaSmjestaj(Smjestaj.USELJEN);
            ZeljaForma f = forma();
            f.setNapomena("    ");

            assertThrows(PrijavaException.class, () -> servis.sacuvajZelju(ID_STUDENTA, f));
        }

        @Test
        @DisplayName("Zahtjev za promjenu s razlogom se snima, razlog bez razmaka na krajevima")
        void poslijeDodjeleSRazlogom() {
            imaPrijavu(Statusi.ZAKLJUCENO);
            imaSmjestaj(Smjestaj.USELJEN);
            ZeljaForma f = forma();
            f.setNapomena("  Zamjena s cimerom zbog rasporeda predavanja  ");

            servis.sacuvajZelju(ID_STUDENTA, f);

            assertEquals("Zamjena s cimerom zbog rasporeda predavanja", sacuvana().getNapomena());
        }

        @Test
        @DisplayName("Nepotvrđen prijedlog rasporeda se ne računa kao dodijeljena soba")
        void prijedlogNijeDodjela() {
            imaPrijavu(Statusi.ODOBRENO);
            imaSmjestaj(Smjestaj.PREDLOZEN);

            assertDoesNotThrow(() -> servis.sacuvajZelju(ID_STUDENTA, forma()));
        }

        @Test
        @DisplayName("Svaka izmjena dobija novo vrijeme (admin dobija novu obavijest)")
        void datumIzmjene() {
            imaPrijavu(Statusi.ODOBRENO);
            LocalDateTime prije = LocalDateTime.now();

            servis.sacuvajZelju(ID_STUDENTA, forma());

            LocalDateTime d = sacuvana().getDatumIzmjene();
            assertNotNull(d);
            assertFalse(d.isBefore(prije));
        }

        @Test
        @DisplayName("Ponovno slanje mijenja postojeću želju, ne pravi novu")
        void izmjenaPostojece() {
            ZeljaSmjestaja postojeca = new ZeljaSmjestaja();
            postojeca.setPrijava(prijava);
            postojeca.setZeljeniKapacitet(3);
            postojeca.setCimer1("stari cimer");
            imaPrijavu(Statusi.ODOBRENO);
            when(zeljaRepository.findByPrijava_IdPrijava(ID_PRIJAVE)).thenReturn(Optional.of(postojeca));

            servis.sacuvajZelju(ID_STUDENTA, forma());

            ZeljaSmjestaja z = sacuvana();
            assertSame(postojeca, z);
            assertEquals(2, z.getZeljeniKapacitet());
            assertNull(z.getCimer1(), "Obrisan cimer u formi mora se obrisati i u bazi");
        }
    }

    // =====================================================================
    // SADRŽAJ ŽELJE
    // =====================================================================

    @Nested
    @DisplayName("Sadržaj želje")
    class Sadrzaj {

        @ParameterizedTest(name = "kapacitet {0} se pamti")
        @ValueSource(ints = {2, 3})
        @DisplayName("Dvokrevetna i trokrevetna se pamte")
        void kapacitetIspravan(int k) {
            imaPrijavu(Statusi.ODOBRENO);
            ZeljaForma f = forma();
            f.setZeljeniKapacitet(k);

            servis.sacuvajZelju(ID_STUDENTA, f);
            assertEquals(k, sacuvana().getZeljeniKapacitet());
        }

        @ParameterizedTest(name = "kapacitet {0} → svejedno")
        @NullSource
        @ValueSource(ints = {0, 1, 4, 10, -2})
        @DisplayName("Ostale vrijednosti znače 'svejedno' (ne greška)")
        void kapacitetSvejedno(Integer k) {
            imaPrijavu(Statusi.ODOBRENO);
            ZeljaForma f = forma();
            f.setZeljeniKapacitet(k);

            servis.sacuvajZelju(ID_STUDENTA, f);
            assertNull(sacuvana().getZeljeniKapacitet());
        }

        @Test
        @DisplayName("Željena soba koja postoji i namijenjena je studentima se pamti")
        void zeljenaSoba() {
            imaPrijavu(Statusi.ODOBRENO);
            Soba s = soba(105, true, true);
            when(sobaRepository.findByBroj(105)).thenReturn(Optional.of(s));
            ZeljaForma f = forma();
            f.setZeljenaSoba(105);

            servis.sacuvajZelju(ID_STUDENTA, f);
            assertSame(s, sacuvana().getSoba());
        }

        @Test
        @DisplayName("Nepostojeća soba se odbija")
        void sobaNePostoji() {
            imaPrijavu(Statusi.ODOBRENO);
            when(sobaRepository.findByBroj(999)).thenReturn(Optional.empty());
            ZeljaForma f = forma();
            f.setZeljenaSoba(999);

            var g = assertThrows(PrijavaException.class, () -> servis.sacuvajZelju(ID_STUDENTA, f));
            assertEquals("Soba 999 ne postoji ili nije za studente.", g.getMessage());
            verify(zeljaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Soba za goste se ne može tražiti")
        void sobaZaGoste() {
            imaPrijavu(Statusi.ODOBRENO);
            when(sobaRepository.findByBroj(1)).thenReturn(Optional.of(soba(1, true, false)));
            ZeljaForma f = forma();
            f.setZeljenaSoba(1);

            assertThrows(PrijavaException.class, () -> servis.sacuvajZelju(ID_STUDENTA, f));
        }

        @Test
        @DisplayName("Neaktivna soba (npr. u renoviranju) se ne može tražiti")
        void neaktivnaSoba() {
            imaPrijavu(Statusi.ODOBRENO);
            when(sobaRepository.findByBroj(105)).thenReturn(Optional.of(soba(105, false, true)));
            ZeljaForma f = forma();
            f.setZeljenaSoba(105);

            assertThrows(PrijavaException.class, () -> servis.sacuvajZelju(ID_STUDENTA, f));
        }

        @Test
        @DisplayName("Bez željene sobe soba se ne traži u bazi")
        void bezZeljeneSobe() {
            imaPrijavu(Statusi.ODOBRENO);
            servis.sacuvajZelju(ID_STUDENTA, forma());

            assertNull(sacuvana().getSoba());
            verifyNoInteractions(sobaRepository);
        }

        @Test
        @DisplayName("Cimeri se pamte bez razmaka, prazno polje kao 'nema'")
        void cimeri() {
            imaPrijavu(Statusi.ODOBRENO);
            ZeljaForma f = forma();
            f.setCimer1("  ZE-2026-015 ");
            f.setCimer2("   ");

            servis.sacuvajZelju(ID_STUDENTA, f);

            ZeljaSmjestaja z = sacuvana();
            assertEquals("ZE-2026-015", z.getCimer1());
            assertNull(z.getCimer2());
        }
    }

    // =====================================================================
    // PRIKAZ NA STRANICI "MOJ SMJEŠTAJ"
    // =====================================================================

    @Nested
    @DisplayName("Prikaz na stranici 'Moj smještaj'")
    class Prikaz {

        @Test
        @DisplayName("Student bez prijave: nema šta prikazati")
        void bezPrijave() {
            when(smjestajRepository.prijaveStudenta(ID_STUDENTA)).thenReturn(List.of());
            assertTrue(servis.stanje(ID_STUDENTA).isEmpty());
        }

        @Test
        @DisplayName("Odobren, bez sobe, želja sačuvana")
        void odobrenBezSobe() {
            imaPrijavu(Statusi.ODOBRENO);
            when(zeljaRepository.findByPrijava_IdPrijava(ID_PRIJAVE)).thenReturn(Optional.of(new ZeljaSmjestaja()));

            MojSmjestaj m = servis.stanje(ID_STUDENTA).orElseThrow();

            assertTrue(m.primljen());
            assertNull(m.soba());
            assertTrue(m.zeljaSacuvana());
        }

        @Test
        @DisplayName("Na pregledu: nije primljen, nema sobe ni želje")
        void naPregledu() {
            imaPrijavu(Statusi.NA_PREGLEDU);

            MojSmjestaj m = servis.stanje(ID_STUDENTA).orElseThrow();

            assertFalse(m.primljen());
            assertNull(m.soba());
            assertFalse(m.zeljaSacuvana());
        }

        @Test
        @DisplayName("Student vidi samo potvrđene cimere, ne sebe i ne nepotvrđene prijedloge")
        void cimeri() {
            imaPrijavu(Statusi.ODOBRENO);
            Soba soba = soba(105, true, true);
            Smjestaj moj = imaSmjestaj(Smjestaj.DODIJELJEN);
            moj.setSoba(soba);

            Smjestaj potvrdjen = stanar(201, Smjestaj.USELJEN, soba);
            Smjestaj prijedlog = stanar(202, Smjestaj.PREDLOZEN, soba);
            when(smjestajRepository.stanariSobe(soba.getIdSoba(), GODINA))
                    .thenReturn(List.of(moj, potvrdjen, prijedlog));

            MojSmjestaj m = servis.stanje(ID_STUDENTA).orElseThrow();

            assertNotNull(m.soba());
            assertEquals(1, m.soba().cimeri().size());
        }

        @Test
        @DisplayName("Forma se popunjava sačuvanom željom")
        void formaPopunjena() {
            imaPrijavu(Statusi.ODOBRENO);
            ZeljaSmjestaja z = new ZeljaSmjestaja();
            z.setZeljeniKapacitet(3);
            z.setSoba(soba(105, true, true));
            z.setCimer1("ZE-2026-015");
            z.setNapomena("Blizu kupatila");
            when(zeljaRepository.findByPrijava_IdPrijava(ID_PRIJAVE)).thenReturn(Optional.of(z));

            ZeljaForma f = servis.zelja(ID_STUDENTA);

            assertEquals(3, f.getZeljeniKapacitet());
            assertEquals(105, f.getZeljenaSoba());
            assertEquals("ZE-2026-015", f.getCimer1());
            assertEquals("Blizu kupatila", f.getNapomena());
        }

        @Test
        @DisplayName("Bez sačuvane želje forma je prazna")
        void formaPrazna() {
            imaPrijavu(Statusi.ODOBRENO);

            ZeljaForma f = servis.zelja(ID_STUDENTA);

            assertNull(f.getZeljeniKapacitet());
            assertNull(f.getZeljenaSoba());
        }
    }

    // ---------- pomoćne ----------

    private void imaPrijavu(String status) {
        prijava.setStatus(status(status));
        when(smjestajRepository.prijaveStudenta(ID_STUDENTA)).thenReturn(List.of(prijava));
    }

    private Smjestaj imaSmjestaj(String status) {
        Smjestaj sm = new Smjestaj();
        sm.setPrijava(prijava);
        sm.setStatus(status);
        when(smjestajRepository.aktivniZaPrijavu(ID_PRIJAVE)).thenReturn(Optional.of(sm));
        return sm;
    }

    private static Smjestaj stanar(int idPrijave, String status, Soba soba) {
        Student s = new Student();
        s.setIme("Cimer");
        s.setPrezime(String.valueOf(idPrijave));
        Prijava p = new Prijava();
        p.setIdPrijava(idPrijave);
        p.setStudent(s);
        Smjestaj sm = new Smjestaj();
        sm.setPrijava(p);
        sm.setStatus(status);
        sm.setSoba(soba);
        return sm;
    }

    private static Soba soba(int broj, boolean aktivna, boolean zaStudente) {
        Sprat sprat = new Sprat();
        sprat.setNaziv("Prvi sprat");
        Soba s = new Soba();
        s.setIdSoba(broj);
        s.setBroj(broj);
        s.setKapacitet(2);
        s.setAktivna(aktivna);
        s.setZaStudente(zaStudente);
        s.setSprat(sprat);
        return s;
    }

    private static ZeljaForma forma() {
        ZeljaForma f = new ZeljaForma();
        f.setZeljeniKapacitet(2);
        return f;
    }

    private static StatusPrijave status(String naziv) {
        StatusPrijave s = new StatusPrijave();
        s.setNaziv(naziv);
        return s;
    }

    private ZeljaSmjestaja sacuvana() {
        ArgumentCaptor<ZeljaSmjestaja> c = ArgumentCaptor.forClass(ZeljaSmjestaja.class);
        verify(zeljaRepository).save(c.capture());
        return c.getValue();
    }
}