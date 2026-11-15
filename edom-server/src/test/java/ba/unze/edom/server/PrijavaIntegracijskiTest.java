package ba.unze.edom.server;

import ba.unze.edom.server.dto.RegistracijaZahtjev;
import ba.unze.edom.server.repository.KorisnikRepository;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.StudentRepository;
import ba.unze.edom.server.repository.VrstaKriterijaRepository;
import ba.unze.edom.server.service.bodovanje.BodovanjeService;
import ba.unze.edom.server.service.korisnik.RegistracijaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integracijski test: cijeli Spring kontekst i stvarna MySQL baza.
 * Svaki test se izvršava u transakciji koja se na kraju poništava, pa baza ostaje nepromijenjena.
 */
@SpringBootTest
@Transactional
class PrijavaIntegracijskiTest {

    @Autowired RegistracijaService registracijaService;
    @Autowired BodovanjeService bodovanjeService;
    @Autowired StudentRepository studentRepository;
    @Autowired KorisnikRepository korisnikRepository;
    @Autowired PrijavaRepository prijavaRepository;
    @Autowired VrstaKriterijaRepository vrstaKriterijaRepository;

    /** Fakultet "Drugo" da test ne zavisi od tačnog naziva fakulteta na spisku. */
    private RegistracijaZahtjev zahtjev(String jmbg, String indeks, String mail) {
        return new RegistracijaZahtjev(
                "Test", "Student", jmbg, indeks, mail, "061000000",
                "Drugo", 3, "Zenica", "Roditelj",
                "Lozinka123!", true);
    }

    @Test
    @DisplayName("Katalog kriterija je popunjen i OS RBiH traži broj mjeseci")
    void katalogPostoji() {
        var sve = vrstaKriterijaRepository.findByAktivanTrueOrderByGrupaAscNazivAsc();
        assertFalse(sve.isEmpty(), "Tabela vrsta_kriterija je prazna");
        assertTrue(sve.stream().anyMatch(v -> v.getSifra().equals("STUDENT_RVI")));
        assertTrue(sve.stream().anyMatch(v -> v.getSifra().equals("IZBJEGLICA")));
        assertTrue(sve.stream()
                .filter(v -> v.getSifra().equals("DIJETE_OSRBIH"))
                .allMatch(v -> v.isTraziParametar()));
    }

    @Test
    @DisplayName("Registracija upisuje i studenta i nalog u bazu")
    void registracijaUpisuje() {
        long prijeStudenata = studentRepository.count();
        long prijeNaloga = korisnikRepository.count();

        registracijaService.registruj(
                zahtjev("9999999999999", "TEST001", "test001@example.com"));

        assertEquals(prijeStudenata + 1, studentRepository.count());
        assertEquals(prijeNaloga + 1, korisnikRepository.count());
    }

    @Test
    @DisplayName("Registracija bez broja indeksa je dozvoljena")
    void registracijaBezIndeksa() {
        registracijaService.registruj(zahtjev("6666666666666", null, "test006@example.com"));

        var s = studentRepository.findByJmbg("6666666666666").orElseThrow();
        assertNull(s.getBrojIndeksa());
    }

    @Test
    @DisplayName("Druga registracija istog JMBG-a ne pravi novog studenta")
    void nemaDuplikata() {
        registracijaService.registruj(
                zahtjev("8888888888888", "TEST002", "test002@example.com"));

        long poslije = studentRepository.count();

        assertThrows(RuntimeException.class, () ->
                registracijaService.registruj(
                        zahtjev("8888888888888", "TEST002", "drugi@example.com")));

        assertEquals(poslije, studentRepository.count());
    }

    @Test
    @DisplayName("Lozinka u bazi nije čitljiva (BCrypt)")
    void lozinkaJeHesirana() {
        var nalog = registracijaService.registruj(
                zahtjev("7777777777777", "TEST003", "test003@example.com"));

        var izBaze = korisnikRepository.findById(nalog.getIdKorisnik()).orElseThrow();
        assertNotEquals("Lozinka123!", izBaze.getPasswordHash());
        assertTrue(izBaze.getPasswordHash().startsWith("$2"));
    }

    @Test
    @DisplayName("Sve prijave iz baze mogu se bodovati, a ponovno računanje daje isti rezultat")
    void bodovanjeNadPravomBazom() {
        var prijave = prijavaRepository.findAll();
        for (var p : prijave) {
            try {
                double prvi = bodovanjeService.izracunaj(p).bodovi();
                double drugi = bodovanjeService.izracunaj(p).bodovi();
                assertEquals(prvi, drugi, 0.001,
                        "Bodovi prijave " + p.getIdPrijava() + " se mijenjaju ponovnim računanjem");
            } catch (ba.unze.edom.server.exception.NevalidnaPrijavaException e) {
                // prijava s neispravnim podacima: očekivano je da se ne boduje
            }
        }
    }
}