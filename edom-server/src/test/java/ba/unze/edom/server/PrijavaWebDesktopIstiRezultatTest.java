package ba.unze.edom.server;

import ba.unze.edom.common.dto.bodovanje.RezultatBodovanja;
import ba.unze.edom.common.dto.prijava.KriterijUnosDTO;
import ba.unze.edom.common.dto.prijava.PrijavaUnosDTO;
import ba.unze.edom.common.dto.prijava.RucniUnosDTO;
import ba.unze.edom.common.dto.prijava.RucniUnosRezultatDTO;
import ba.unze.edom.common.dto.student.StudentUnosDTO;
import ba.unze.edom.server.dto.DomacinstvoKorakDTO;
import ba.unze.edom.server.dto.KriterijIzborDTO;
import ba.unze.edom.server.dto.StudijKorakDTO;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.VrstaKriterijaRepository;
import ba.unze.edom.server.service.bodovanje.BodovanjeService;
import ba.unze.edom.server.service.obavijesti.EmailService;
import ba.unze.edom.server.service.opste.PostavkeService;
import ba.unze.edom.server.service.prijava.PrijavaService;
import ba.unze.edom.server.service.prijava.RucniUnosService;
import ba.unze.edom.server.service.prijava.Statusi;
import ba.unze.edom.server.service.student.StudentEvidencijaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.doReturn;

/**
 * Prijava podnesena na webu i prijava unesena s papira na desktopu
 * moraju proći kroz ista pravila i dati isti rezultat.
 *
 * Isti podaci se unose dva puta, pravim servisima i stvarnom bazom:
 *  - WEB:     kreirajIliNastavi → korak 1 → korak 2 → korak 3 → podnesi
 *  - DESKTOP: rucni-unos (RucniUnosService.unesi)
 * pa se uporede bodovi, stavke, status i snimljeni podaci.
 *
 * Sve se dešava u transakciji koja se na kraju poništava. Akademska godina 2099
 * je izabrana da se ne miješa s pravim prijavama. E-mail se ne šalje (EmailService je mock).
 */
@SpringBootTest
@Transactional
class PrijavaWebDesktopIstiRezultatTest {

    private static final int GODINA = 2099;
    private static final String KANTON = "Zeničko-dobojski kanton";
    private static final String OPCINA = "Zenica";

    @Autowired PrijavaService prijavaService;
    @Autowired RucniUnosService rucniUnosService;
    @Autowired StudentEvidencijaService studentEvidencija;
    @Autowired BodovanjeService bodovanjeService;
    @Autowired PrijavaRepository prijavaRepository;
    @Autowired VrstaKriterijaRepository vrstaKriterijaRepository;

    @MockitoSpyBean PostavkeService postavke;
    @MockitoBean EmailService emailService;

    @BeforeEach
    void konkursOtvoren() {
        // web prijava radi samo dok traje konkurs; za test ga "otvaramo"
        doReturn(true).when(postavke).prijaveOtvorene();
    }

    // =====================================================================
    // ISTI PODACI → ISTI REZULTAT
    // =====================================================================

    @Nested
    @DisplayName("Isti podaci daju isti rezultat")
    class IstiRezultat {

        @Test
        @DisplayName("Student 3. godine s kriterijima: isti bodovi, stavke, status i podaci")
        void starijiSKriterijima() {
            Podaci d = new Podaci(3, "8.50", 20, 4, "1200", "95", 1, Map.of(
                    "STUDENT_RVI", "60", "DIJETE_OSRBIH", "44", "IZBJEGLICA", ""));
            uporedi(d, "0202999171011", "0303999171020");
        }

        @Test
        @DisplayName("Brucoš bez kriterija")
        void brucos() {
            Podaci d = new Podaci(1, "4.50", null, 3, "600", "130", 1, Map.of());
            uporedi(d, "0202999171011", "0303999171020");
        }

        @Test
        @DisplayName("Student koji ponovo upisuje godinu (−3 boda)")
        void ponovniUpis() {
            Podaci d = new Podaci(4, "7.20", 25, 5, "800", "60", 2, Map.of("BEZ_JEDNOG_RODIT", ""));
            uporedi(d, "0202999171011", "0303999171020");
        }

        @Test
        @DisplayName("Desktop pregled bodova prije snimanja = bodovi nakon snimanja")
        void pregledJednakUnosu() {
            Podaci d = new Podaci(3, "8.50", 20, 4, "1200", "95", 1, Map.of("STUDENT_RVI", "60"));
            RucniUnosDTO unos = desktopUnos(d, "0404999171030");

            RezultatBodovanja pregled = rucniUnosService.pregledBodova(unos);
            RucniUnosRezultatDTO r = rucniUnosService.unesi(unos);

            assertEquals(pregled.bodovi(), r.bodovi(), 0.001);
        }

        private void uporedi(Podaci d, String jmbgWeb, String jmbgDesktop) {
            Prijava web = prijavaRepository.findById(webPrijava(d, jmbgWeb)).orElseThrow();
            Prijava desk = prijavaRepository.findById(
                    rucniUnosService.unesi(desktopUnos(d, jmbgDesktop)).idPrijava()).orElseThrow();

            RezultatBodovanja rWeb = bodovanjeService.izracunaj(web);
            RezultatBodovanja rDesk = bodovanjeService.izracunaj(desk);

            assertEquals(web.getUkupniBodovi(), desk.getUkupniBodovi(), 0.001, "Snimljeni bodovi se razlikuju");
            // redoslijed kriterija na webu i desktopu može biti različit, pa se porede kao skup
            assertEquals(Set.copyOf(rWeb.stavke()), Set.copyOf(rDesk.stavke()), "Stavke bodovanja se razlikuju");
            assertEquals(rWeb.stavke().size(), rDesk.stavke().size());

            assertEquals(Statusi.NA_PREGLEDU, web.getStatus().getNaziv());
            assertEquals(Statusi.NA_PREGLEDU, desk.getStatus().getNaziv());

            assertEquals(web.getGodinaStudija(), desk.getGodinaStudija());
            assertEquals(web.getUpisGodinePut(), desk.getUpisGodinePut());
            assertEquals(0, web.getProsjek().compareTo(desk.getProsjek()));
            assertEquals(web.getPolozeniIspiti(), desk.getPolozeniIspiti());
            assertEquals(web.getBrojClanovaDomacinstva(), desk.getBrojClanovaDomacinstva());
            assertEquals(0, web.getUkupnaPrimanja().compareTo(desk.getUkupnaPrimanja()));
            assertEquals(0, web.getUdaljenostKm().compareTo(desk.getUdaljenostKm()));
            assertEquals(web.getKriteriji().size(), desk.getKriteriji().size());

            Student sw = web.getStudent(), sd = desk.getStudent();
            assertEquals(sw.getBrojLicneKarte(), sd.getBrojLicneKarte());
            assertEquals(sw.getTelefonRoditelja(), sd.getTelefonRoditelja());
            assertEquals(sw.getOpcina(), sd.getOpcina());
            assertEquals(sw.getSpol(), sd.getSpol());
        }
    }

    // =====================================================================
    // ISTI NEISPRAVNI PODACI → ISTA GREŠKA
    // =====================================================================

    @Nested
    @DisplayName("Neispravni podaci se odbijaju isto")
    class IstaGreska {

        @Test
        @DisplayName("Brucoš s fakultetskim prosjekom (9,5)")
        void pogresnaSkala() {
            istaGreska(new Podaci(1, "9.50", null, 4, "1000", "95", 1, Map.of()));
        }

        @Test
        @DisplayName("Treća godina s prosjekom ispod 6")
        void prosjekIspodSest() {
            istaGreska(new Podaci(3, "5.50", 20, 4, "1000", "95", 1, Map.of()));
        }

        @Test
        @DisplayName("Invalidnost preko 100%")
        void invalidnostPreko100() {
            istaGreska(new Podaci(3, "8.00", 20, 4, "1000", "95", 1, Map.of("STUDENT_RVI", "150")));
        }

        @Test
        @DisplayName("Nerealan broj mjeseci u OS RBiH")
        void previseMjeseci() {
            istaGreska(new Podaci(3, "8.00", 20, 4, "1000", "95", 1, Map.of("DIJETE_OSRBIH", "9999")));
        }

        @Test
        @DisplayName("Previše položenih ispita")
        void previseIspita() {
            istaGreska(new Podaci(3, "8.00", 150, 4, "1000", "95", 1, Map.of()));
        }

        @Test
        @DisplayName("Godina upisana više od 4 puta")
        void previseUpisa() {
            istaGreska(new Podaci(3, "8.00", 20, 4, "1000", "95", 5, Map.of()));
        }

        @Test
        @DisplayName("Parametarski kriterij bez parametra")
        void kriterijBezParametra() {
            istaGreska(new Podaci(3, "8.00", 20, 4, "1000", "95", 1, Map.of("STUDENT_RVI", "")));
        }

        @Test
        @DisplayName("Nedostaju primanja domaćinstva")
        void bezPrimanja() {
            istaGreska(new Podaci(3, "8.00", 20, 4, null, "95", 1, Map.of()));
        }

        @Test
        @DisplayName("Udaljenost manja od minimuma iz uslova konkursa")
        void premalaUdaljenost() {
            Integer min = postavke.trenutne().minUdaljenostKm();
            assumeTrue(min != null && min > 0, "U postavkama nije zadana minimalna udaljenost");
            istaGreska(new Podaci(3, "8.00", 20, 4, "1000", String.valueOf(min - 1), 1, Map.of()));
        }

        private void istaGreska(Podaci d) {
            String webGreska = assertThrows(RuntimeException.class,
                    () -> webPrijava(d, "0505999175044"), "Web je prihvatio neispravne podatke").getMessage();
            String deskGreska = assertThrows(RuntimeException.class,
                    () -> rucniUnosService.unesi(desktopUnos(d, "0606999171058")),
                    "Desktop je prihvatio neispravne podatke").getMessage();

            assertEquals(webGreska, deskGreska, "Web i desktop javljaju različite greške");
        }
    }

    // =====================================================================
    // ISTI STUDENT, JEDNA PRIJAVA GODIŠNJE
    // =====================================================================

    @Nested
    @DisplayName("Isti student i jedna prijava godišnje")
    class Student1 {

        private final Podaci d = new Podaci(3, "8.50", 20, 4, "1200", "95", 1, Map.of());

        @Test
        @DisplayName("Student registrovan na webu: desktop ga pronalazi po JMBG-u, ne pravi novog")
        void istiStudent() {
            Student s = studentEvidencija.nadjiIliKreiraj(student("0707999175062", 3));

            RucniUnosDTO unos = desktopUnos(d, "0707999175062");
            assertEquals(s.getIdStudent(), rucniUnosService.unesi(unos).idStudent());
        }

        @Test
        @DisplayName("Prijava započeta na webu ('u izradi'): sekretar je preuzima s papira")
        void preuzimaZapocetu() {
            Student s = studentEvidencija.nadjiIliKreiraj(student("0707999175062", 3));
            Prijava zapoceta = prijavaService.kreirajIliNastavi(s.getIdStudent(), GODINA);

            RucniUnosRezultatDTO r = rucniUnosService.unesi(desktopUnos(d, "0707999175062"));

            assertEquals(zapoceta.getIdPrijava(), r.idPrijava(), "Trebala je biti preuzeta ista prijava");
            Prijava p = prijavaRepository.findById(r.idPrijava()).orElseThrow();
            assertEquals(Statusi.NA_PREGLEDU, p.getStatus().getNaziv());
            // i dalje tačno jedna prijava za tu godinu (upit bi pao da ih ima dvije)
            assertTrue(prijavaRepository
                    .findByStudent_IdStudentAndAkademskaGodina(s.getIdStudent(), GODINA).isPresent());
        }

        @Test
        @DisplayName("Već podnesena web prijava: desktop odbija drugu za istu godinu")
        void podnesenaSeNeMijenja() {
            webPrijava(d, "0707999175062");

            var greska = assertThrows(RuntimeException.class,
                    () -> rucniUnosService.unesi(desktopUnos(d, "0707999175062")));
            assertTrue(greska.getMessage().contains("već ima prijavu"));
        }

        @Test
        @DisplayName("Već unesena papirna prijava: student je ne može ponovo započeti na webu")
        void webNakonDesktopa() {
            RucniUnosRezultatDTO r = rucniUnosService.unesi(desktopUnos(d, "0707999175062"));

            var greska = assertThrows(RuntimeException.class,
                    () -> prijavaService.kreirajIliNastavi(r.idStudent(), GODINA));
            assertTrue(greska.getMessage().contains("Već ste podnijeli"));
        }
    }

    // =====================================================================
    // pomoćne: isti podaci, dva puta
    // =====================================================================

    /** Podaci jedne prijave. Kriteriji: šifra → parametar ("" = bez parametra). */
    record Podaci(int godina, String prosjek, Integer ispiti, int clanova, String primanja,
                  String km, int upisPut, Map<String, String> kriteriji) {}

    private StudentUnosDTO student(String jmbg, int godina) {
        return new StudentUnosDTO(
                "Test", "Student", jmbg, null, "test" + jmbg + "@example.com", "061111222",
                "Drugo", godina, "Zenica, Ulica 1", "Roditelj", null,
                "1e2345678", "061222333", KANTON, OPCINA);
    }

    /** Prijava kroz web, korak po korak, kao student u pregledniku. Vraća id prijave. */
    private Integer webPrijava(Podaci d, String jmbg) {
        Student s = studentEvidencija.nadjiIliKreiraj(student(jmbg, d.godina()));
        Integer idS = s.getIdStudent();
        Integer id = prijavaService.kreirajIliNastavi(idS, GODINA).getIdPrijava();

        prijavaService.sacuvajStudij(id, idS, new StudijKorakDTO(
                GODINA, d.godina(), new BigDecimal(d.prosjek()), d.ispiti(), d.upisPut(),
                "1e2345678", "061222333", KANTON, OPCINA));

        prijavaService.sacuvajDomacinstvo(id, idS, new DomacinstvoKorakDTO(
                d.clanova(), d.primanja() != null ? new BigDecimal(d.primanja()) : null,
                new BigDecimal(d.km())));

        var forma = prijavaService.pripremiKriterije(id, idS);
        d.kriteriji().forEach((sifra, param) -> {
            Integer idVrste = vrstaKriterijaRepository.findBySifra(sifra).orElseThrow().getIdVrstaKriterija();
            KriterijIzborDTO red = forma.getKriteriji().stream()
                    .filter(k -> k.getIdVrsteKriterija().equals(idVrste)).findFirst().orElseThrow();
            red.setIzabran(true);
            red.setParametar(param.isEmpty() ? null : new BigDecimal(param));
        });
        prijavaService.sacuvajKriterije(id, idS, forma);

        prijavaService.podnesi(id, idS, "Test napomena");
        return id;
    }

    /** Isti podaci kako ih sekretar unosi na desktopu. */
    private RucniUnosDTO desktopUnos(Podaci d, String jmbg) {
        List<KriterijUnosDTO> kriteriji = d.kriteriji().entrySet().stream()
                .map(e -> new KriterijUnosDTO(e.getKey(),
                        e.getValue().isEmpty() ? null : new BigDecimal(e.getValue())))
                .toList();

        return new RucniUnosDTO(
                student(jmbg, d.godina()),
                new PrijavaUnosDTO(GODINA, new BigDecimal(d.prosjek()), d.ispiti(), d.clanova(),
                        d.primanja() != null ? new BigDecimal(d.primanja()) : null,
                        new BigDecimal(d.km()), "Test napomena", d.upisPut()),
                kriteriji,
                List.of());
    }
}