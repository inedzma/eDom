package ba.unze.edom.server.service.obavijesti;

import ba.unze.edom.common.dto.opste.ObavijestDTO;
import ba.unze.edom.common.dto.student.StudentUnosDTO;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.SobaRepository;
import ba.unze.edom.server.repository.StatusPrijaveRepository;
import ba.unze.edom.server.repository.ZeljaSmjestajaRepository;
import ba.unze.edom.server.service.prijava.Statusi;
import ba.unze.edom.server.service.student.StudentEvidencijaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Obavijest administratoru kad student zatraži promjenu sobe.
 * Obavijest postoji samo ako je student već dobio sobu (DODIJELJEN ili USELJEN)
 * i izmijenio želju u zadnjih 30 dana; svaka nova izmjena je nova obavijest.
 *
 * Pravi upit nad bazom; podaci se upisuju u transakciji koja se na kraju poništava.
 * Koristi se akademska godina 2099 da se ne miješa s pravim prijavama.
 */
@SpringBootTest
@Transactional
class PromjenaSobeObavijestiTest {

    private static final int GODINA = 2099;

    @Autowired PromjenaSobeObavijesti izvor;
    @Autowired StudentEvidencijaService studentEvidencija;
    @Autowired PrijavaRepository prijavaRepository;
    @Autowired StatusPrijaveRepository statusRepository;
    @Autowired SobaRepository sobaRepository;
    @Autowired ZeljaSmjestajaRepository zeljaRepository;
    @Autowired EntityManager em;

    private Soba soba;

    @BeforeEach
    void pripremi() {
        soba = sobaRepository.sveSaSpratom().stream().filter(Soba::zaRaspored).findFirst().orElse(null);
        assumeTrue(soba != null, "U bazi nema nijedne sobe za studente");
    }

    // =====================================================================
    // KADA SE OBAVIJEST POJAVLJUJE
    // =====================================================================

    @ParameterizedTest(name = "smještaj {0}: admin dobija obavijest")
    @ValueSource(strings = {Smjestaj.DODIJELJEN, Smjestaj.USELJEN})
    @DisplayName("Student sa sobom traži promjenu: admin dobija obavijest")
    void saSobom(String statusSmjestaja) {
        Prijava p = prijava("0202999171011");
        smjestaj(p, statusSmjestaja);
        ZeljaSmjestaja z = zelja(p, "Zamjena s cimerom", LocalDateTime.now());

        assertNotNull(nadji(z), "Nema obavijesti za zahtjev");
    }

    @ParameterizedTest(name = "smještaj {0}: nema obavijesti")
    @ValueSource(strings = {Smjestaj.PREDLOZEN, Smjestaj.ISELJEN})
    @DisplayName("Nepotvrđen prijedlog ili iseljen student: nema obavijesti")
    void bezPotvrdjeneSobe(String statusSmjestaja) {
        Prijava p = prijava("0202999171011");
        smjestaj(p, statusSmjestaja);
        ZeljaSmjestaja z = zelja(p, "Neka napomena", LocalDateTime.now());

        assertNull(nadji(z));
    }

    @Test
    @DisplayName("Želja prije dodjele sobe nije zahtjev za promjenu: nema obavijesti")
    void prijeDodjele() {
        Prijava p = prijava("0202999171011");
        ZeljaSmjestaja z = zelja(p, null, LocalDateTime.now());

        assertNull(nadji(z));
    }

    @Test
    @DisplayName("Zahtjev izmijenjen prije 29 dana se još prikazuje")
    void prije29Dana() {
        Prijava p = prijava("0202999171011");
        smjestaj(p, Smjestaj.DODIJELJEN);
        ZeljaSmjestaja z = zelja(p, "Razlog", LocalDateTime.now().minusDays(29));

        assertNotNull(nadji(z));
    }

    @Test
    @DisplayName("Zahtjev stariji od 30 dana se više ne prikazuje")
    void starije() {
        Prijava p = prijava("0202999171011");
        smjestaj(p, Smjestaj.DODIJELJEN);
        ZeljaSmjestaja z = zelja(p, "Razlog", LocalDateTime.now().minusDays(31));

        assertNull(nadji(z));
    }

    // =====================================================================
    // SADRŽAJ OBAVIJESTI
    // =====================================================================

    @Test
    @DisplayName("Obavijest sadrži ime studenta, željenu sobu, cimere i razlog")
    void sadrzaj() {
        Prijava p = prijava("0202999171011");
        smjestaj(p, Smjestaj.USELJEN);
        ZeljaSmjestaja z = zelja(p, "Zamjena s cimerom zbog rasporeda", LocalDateTime.now());
        z.setSoba(soba);
        z.setCimer1("ZE-2099-001");
        z.setCimer2("ana@example.com");
        em.flush();

        String o = nadji(z);
        assertNotNull(o);
        assertTrue(o.contains("Promjena sobe: Test Student"));
        assertTrue(o.contains("željena soba " + soba.getBroj()));
        assertTrue(o.contains("cimer ZE-2099-001"));
        assertTrue(o.contains("cimer ana@example.com"));
        assertTrue(o.contains("Zamjena s cimerom zbog rasporeda"));
    }

    @Test
    @DisplayName("Dug razlog se skraćuje na 120 znakova")
    void dugRazlog() {
        Prijava p = prijava("0202999171011");
        smjestaj(p, Smjestaj.DODIJELJEN);
        String dug = "R".repeat(200);
        ZeljaSmjestaja z = zelja(p, dug, LocalDateTime.now());

        String o = nadji(z);
        assertNotNull(o);
        assertTrue(o.contains("R".repeat(119) + "…"));
        assertFalse(o.contains("R".repeat(120)));
    }

    @Test
    @DisplayName("Nova izmjena istog zahtjeva je nova obavijest (admin je ponovo vidi)")
    void novaIzmjena() {
        Prijava p = prijava("0202999171011");
        smjestaj(p, Smjestaj.DODIJELJEN);
        ZeljaSmjestaja z = zelja(p, "Prvi razlog", LocalDateTime.now().minusDays(2));
        String staraOznaka = oznaka(z);

        z.setNapomena("Drugi razlog");
        z.setDatumIzmjene(LocalDateTime.now());
        em.flush();

        List<String> sve = sve();
        assertTrue(sve.stream().anyMatch(o -> o.contains(oznaka(z))), "Nema nove obavijesti");
        assertTrue(sve.stream().noneMatch(o -> o.contains(staraOznaka)),
                "Stara oznaka se ne smije ponoviti");
        assertNotEquals(staraOznaka, oznaka(z));
    }

    @Test
    @DisplayName("Noviji zahtjevi su na vrhu liste")
    void redoslijed() {
        Prijava starija = prijava("0202999171011");
        smjestaj(starija, Smjestaj.DODIJELJEN);
        ZeljaSmjestaja zStara = zelja(starija, "Stariji zahtjev", LocalDateTime.now().minusDays(5));

        Prijava novija = prijava("0303999171020");
        smjestaj(novija, Smjestaj.DODIJELJEN);
        ZeljaSmjestaja zNova = zelja(novija, "Noviji zahtjev", LocalDateTime.now());

        List<String> sve = sve();
        int iNova = indeks(sve, zNova), iStara = indeks(sve, zStara);
        assertTrue(iNova >= 0 && iStara >= 0);
        assertTrue(iNova < iStara, "Noviji zahtjev mora biti iznad starijeg");
    }

    // ---------- pomoćne: podaci u bazi ----------

    private Prijava prijava(String jmbg) {
        Student s = studentEvidencija.nadjiIliKreiraj(new StudentUnosDTO(
                "Test", "Student", jmbg, null, "test" + jmbg + "@example.com", "061111222",
                "Drugo", 3, "Zenica, Ulica 1", "Roditelj", null,
                "1E2345678", "061222333", "Zeničko-dobojski kanton", "Zenica"));

        Prijava p = new Prijava();
        p.setStudent(s);
        p.setAkademskaGodina(GODINA);
        p.setDatumPrijave(LocalDate.now());
        p.setGodinaStudija(3);
        p.setUkupniBodovi(0.0);
        p.setStatus(statusRepository.findByNaziv(Statusi.ODOBRENO).orElseThrow());
        return prijavaRepository.save(p);
    }

    private void smjestaj(Prijava p, String status) {
        Smjestaj sm = new Smjestaj();
        sm.setPrijava(p);
        sm.setSoba(soba);
        sm.setStatus(status);
        em.persist(sm);
    }

    private ZeljaSmjestaja zelja(Prijava p, String napomena, LocalDateTime izmjena) {
        ZeljaSmjestaja z = new ZeljaSmjestaja();
        z.setPrijava(p);
        z.setNapomena(napomena);
        z.setDatumIzmjene(izmjena);
        zeljaRepository.save(z);
        em.flush();
        return z;
    }

    // ---------- pomoćne: obavijesti ----------
    // ObavijestDTO je record, pa njegov toString sadrži sva polja; tako test ne zavisi od naziva polja.

    private List<String> sve() {
        return izvor.obavijesti().stream().map(ObavijestDTO::toString).toList();
    }

    private static String oznaka(ZeljaSmjestaja z) {
        return "ZAHTJEV_SOBA:" + z.getIdZelja() + ":" + z.getDatumIzmjene();
    }

    /** Obavijest za ovu želju (kao tekst) ili null. */
    private String nadji(ZeljaSmjestaja z) {
        String prefiks = "ZAHTJEV_SOBA:" + z.getIdZelja() + ":";
        return sve().stream().filter(o -> o.contains(prefiks)).findFirst().orElse(null);
    }

    private int indeks(List<String> sve, ZeljaSmjestaja z) {
        String prefiks = "ZAHTJEV_SOBA:" + z.getIdZelja() + ":";
        for (int i = 0; i < sve.size(); i++) if (sve.get(i).contains(prefiks)) return i;
        return -1;
    }
}