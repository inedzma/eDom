package ba.unze.edom.server;

import ba.unze.edom.server.dto.RegistracijaZahtjev;
import ba.unze.edom.server.repository.*;
import ba.unze.edom.server.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional          // svaki test se poništava na kraju
class PrijavaIntegracijskiTest {

    @Autowired RegistracijaService registracijaService;
    @Autowired BodovanjeService bodovanjeService;
    @Autowired StudentRepository studentRepository;
    @Autowired KorisnikRepository korisnikRepository;
    @Autowired PrijavaRepository prijavaRepository;
    @Autowired VrstaKriterijaRepository vrstaKriterijaRepository;

    private RegistracijaZahtjev zahtjev(String jmbg, String indeks, String mail) {
        return new RegistracijaZahtjev(
                "Test", "Student", jmbg, indeks, mail, "061000000",
                "Politehnicki fakultet UNZE", 3, "Zenica", "Roditelj",
                "lozinka123");
    }

    @Test
    @DisplayName("Katalog kriterija je popunjen")
    void katalogPostoji() {
        var sve = vrstaKriterijaRepository.findByAktivanTrueOrderByGrupaAscNazivAsc();
        assertFalse(sve.isEmpty(), "Tabela vrsta_kriterija je prazna");
        assertTrue(sve.stream().anyMatch(v -> v.getSifra().equals("STUDENT_RVI")));
        assertTrue(sve.stream().anyMatch(v -> v.getSifra().equals("IZBJEGLICA")));
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
    @DisplayName("Lozinka u bazi nije citljiva")
    void lozinkaJeHesirana() {
        var nalog = registracijaService.registruj(
                zahtjev("7777777777777", "TEST003", "test003@example.com"));

        var izBaze = korisnikRepository.findById(nalog.getIdKorisnik()).orElseThrow();
        assertNotEquals("lozinka123", izBaze.getPasswordHash());
        assertTrue(izBaze.getPasswordHash().startsWith("$2a$"));
    }

    @Test
    @DisplayName("Bodovi se racunaju iz prijave ucitane iz baze")
    void bodovanjeNadPravomBazom() {
        var prijave = prijavaRepository.findAll();
        if (prijave.isEmpty()) return;   // prazna baza, preskoci

        var p = prijave.get(0);
        assertDoesNotThrow(() -> bodovanjeService.izracunaj(p));
    }

    @Test
    @DisplayName("Ponovljeno racunanje daje isti rezultat")
    void racunanjeJeDeterministicko() {
        var prijave = prijavaRepository.findAll();
        if (prijave.isEmpty()) return;

        var p = prijave.get(0);
        var prvi = bodovanjeService.izracunaj(p);
        var drugi = bodovanjeService.izracunaj(p);

        assertEquals(prvi.potvrdjeno(), drugi.potvrdjeno(),
                "Bodovi se ne smiju mijenjati ponovnim racunanjem");
    }
}