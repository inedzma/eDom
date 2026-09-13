package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.RegistracijaZahtjev;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistracijaServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock KorisnikRepository korisnikRepository;
    @Mock UlogaRepository ulogaRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks RegistracijaService service;

    private Uloga ulogaStudent;

    @BeforeEach
    void pripremi() {
        ulogaStudent = new Uloga();
        ulogaStudent.setIdUloga(1);
        ulogaStudent.setNaziv("Student");
    }

    private RegistracijaZahtjev zahtjev() {
        return new RegistracijaZahtjev(
                "Merjem", "Grizic", "1234567890123", "320",
                "merjem@example.com", "061111222", "Politehnicki fakultet UNZE",
                3, "Zenica, Ulica 1", "Adem", "lozinka123");
    }

    private Student postojeciStudent(int id) {
        Student s = new Student();
        s.setIdStudent(id);
        s.setIme("Merjem");
        s.setPrezime("Grizic");
        s.setJmbg("1234567890123");
        s.setBrojIndeksa("320");
        return s;
    }

    // ---------- GRANA 1: potpuno nov student ----------

    @Test
    @DisplayName("Nov student: kreira se i student i nalog")
    void novStudent() {
        when(studentRepository.findByJmbg(any())).thenReturn(Optional.empty());
        when(studentRepository.findByBrojIndeksa(any())).thenReturn(Optional.empty());
        when(korisnikRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(ulogaRepository.findByNaziv("Student")).thenReturn(Optional.of(ulogaStudent));
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        when(korisnikRepository.save(any(Korisnik.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.registruj(zahtjev());

        // kljucno: novi student JE sacuvan
        verify(studentRepository).save(any(Student.class));
        verify(korisnikRepository).save(any(Korisnik.class));
    }

    // ---------- GRANA 2: postojeci student bez naloga ----------

    @Test
    @DisplayName("Postojeci student bez naloga: NE kreira se novi student")
    void postojeciStudentDobijaNalog() {
        Student postojeci = postojeciStudent(42);

        when(studentRepository.findByJmbg("1234567890123"))
                .thenReturn(Optional.of(postojeci));
        when(korisnikRepository.findByStudent_IdStudent(42))
                .thenReturn(Optional.empty());
        when(korisnikRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(ulogaRepository.findByNaziv("Student")).thenReturn(Optional.of(ulogaStudent));
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");

        ArgumentCaptor<Korisnik> captor = ArgumentCaptor.forClass(Korisnik.class);
        when(korisnikRepository.save(captor.capture()))
                .thenAnswer(i -> i.getArgument(0));

        service.registruj(zahtjev());

        // OVO je srz: nema duplikata
        verify(studentRepository, never()).save(any(Student.class));

        // nalog je vezan bas na postojeceg studenta
        assertEquals(42, captor.getValue().getStudent().getIdStudent());
    }

    @Test
    @DisplayName("Postojecem studentu se azuriraju kontakt podaci")
    void azuriranjeKontakta() {
        Student postojeci = postojeciStudent(42);
        postojeci.setEmail("staro@example.com");

        when(studentRepository.findByJmbg(any())).thenReturn(Optional.of(postojeci));
        when(korisnikRepository.findByStudent_IdStudent(42)).thenReturn(Optional.empty());
        when(korisnikRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(ulogaRepository.findByNaziv("Student")).thenReturn(Optional.of(ulogaStudent));
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        when(korisnikRepository.save(any(Korisnik.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.registruj(zahtjev());

        assertEquals("merjem@example.com", postojeci.getEmail());
    }

    // ---------- GRANA 3: student vec ima nalog ----------

    @Test
    @DisplayName("Student koji vec ima nalog dobija gresku")
    void vecImaNalog() {
        Student postojeci = postojeciStudent(42);
        Korisnik postojeciNalog = new Korisnik();

        when(studentRepository.findByJmbg(any())).thenReturn(Optional.of(postojeci));
        when(korisnikRepository.findByStudent_IdStudent(42))
                .thenReturn(Optional.of(postojeciNalog));

        var greska = assertThrows(RegistracijaException.class,
                () -> service.registruj(zahtjev()));

        assertTrue(greska.getMessage().contains("nalog"));
        verify(korisnikRepository, never()).save(any());
    }

    // ---------- SIGURNOSNI SLUCAJ ----------

    @Test
    @DisplayName("Indeks postoji uz drugi JMBG - registracija se odbija")
    void indeksSaDrugimJmbg() {
        when(studentRepository.findByJmbg(any())).thenReturn(Optional.empty());
        when(studentRepository.findByBrojIndeksa("320"))
                .thenReturn(Optional.of(postojeciStudent(99)));

        assertThrows(RegistracijaException.class,
                () -> service.registruj(zahtjev()));

        verify(studentRepository, never()).save(any(Student.class));
        verify(korisnikRepository, never()).save(any());
    }

    @Test
    @DisplayName("Zauzet e-mail se odbija")
    void zauzetEmail() {
        when(studentRepository.findByJmbg(any())).thenReturn(Optional.empty());
        when(studentRepository.findByBrojIndeksa(any())).thenReturn(Optional.empty());
        when(korisnikRepository.findByEmail("merjem@example.com"))
                .thenReturn(Optional.of(new Korisnik()));

        assertThrows(RegistracijaException.class,
                () -> service.registruj(zahtjev()));
    }

    @Test
    @DisplayName("Lozinka se nikad ne cuva u citljivom obliku")
    void lozinkaSeHesira() {
        when(studentRepository.findByJmbg(any())).thenReturn(Optional.empty());
        when(studentRepository.findByBrojIndeksa(any())).thenReturn(Optional.empty());
        when(korisnikRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(ulogaRepository.findByNaziv("Student")).thenReturn(Optional.of(ulogaStudent));
        when(passwordEncoder.encode("lozinka123")).thenReturn("$2a$10$hash");

        ArgumentCaptor<Korisnik> captor = ArgumentCaptor.forClass(Korisnik.class);
        when(korisnikRepository.save(captor.capture()))
                .thenAnswer(i -> i.getArgument(0));

        service.registruj(zahtjev());

        assertNotEquals("lozinka123", captor.getValue().getPasswordHash());
        assertTrue(captor.getValue().getPasswordHash().startsWith("$2a$"));
    }
}