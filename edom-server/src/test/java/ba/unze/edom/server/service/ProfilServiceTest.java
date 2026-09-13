package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.ProfilIzmjenaDTO;
import ba.unze.edom.server.entity.Korisnik;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.repository.KorisnikRepository;
import ba.unze.edom.server.repository.StudentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfilServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock KorisnikRepository korisnikRepository;
    @InjectMocks ProfilService service;

    private Student student;
    private Korisnik korisnik;

    @BeforeEach
    void pripremi() {
        student = new Student();
        student.setIdStudent(1);
        student.setIme("Merjem");
        student.setEmail("staro@example.com");
        student.setAdresa("Zenica");
        student.setProsjek(new BigDecimal("8.50"));

        korisnik = new Korisnik();
        korisnik.setIdKorisnik(10);
        korisnik.setEmail("staro@example.com");
        korisnik.setUsername("staro@example.com");
    }

    private ProfilIzmjenaDTO izmjena(String email) {
        return new ProfilIzmjenaDTO(email, "061222333",
                "Sarajevo, Ulica 5", "Ekonomski fakultet UNZE", 4);
    }

    @Test
    @DisplayName("Promjena e-maila mijenja i username")
    void emailMijenjaUsername() {
        when(korisnikRepository.findById(10)).thenReturn(Optional.of(korisnik));
        when(korisnikRepository.findByEmail("novo@example.com"))
                .thenReturn(Optional.empty());
        when(studentRepository.findById(1)).thenReturn(Optional.of(student));

        service.sacuvaj(10, 1, izmjena("novo@example.com"));

        assertEquals("novo@example.com", korisnik.getEmail());
        assertEquals("novo@example.com", korisnik.getUsername());
        assertEquals("novo@example.com", student.getEmail());
    }

    @Test
    @DisplayName("Zauzet e-mail se odbija")
    void zauzetEmail() {
        when(korisnikRepository.findById(10)).thenReturn(Optional.of(korisnik));
        when(korisnikRepository.findByEmail("tudji@example.com"))
                .thenReturn(Optional.of(new Korisnik()));

        assertThrows(RegistracijaException.class,
                () -> service.sacuvaj(10, 1, izmjena("tudji@example.com")));
    }

    @Test
    @DisplayName("Prosjek se ne mijenja kroz profil")
    void prosjekOstajeNetaknut() {
        when(korisnikRepository.findById(10)).thenReturn(Optional.of(korisnik));
        when(studentRepository.findById(1)).thenReturn(Optional.of(student));

        service.sacuvaj(10, 1, izmjena("staro@example.com"));

        assertEquals(new BigDecimal("8.50"), student.getProsjek());
    }

    @Test
    @DisplayName("Ostala polja se azuriraju")
    void ostalaPolja() {
        when(korisnikRepository.findById(10)).thenReturn(Optional.of(korisnik));
        when(studentRepository.findById(1)).thenReturn(Optional.of(student));

        service.sacuvaj(10, 1, izmjena("staro@example.com"));

        assertEquals("Sarajevo, Ulica 5", student.getAdresa());
        assertEquals(4, student.getGodinaStudija());
    }
}