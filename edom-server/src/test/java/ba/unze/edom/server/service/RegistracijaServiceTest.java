package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.student.StudentUnosDTO;
import ba.unze.edom.server.dto.RegistracijaZahtjev;
import ba.unze.edom.server.entity.Korisnik;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.entity.Uloga;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.repository.KorisnikRepository;
import ba.unze.edom.server.repository.UlogaRepository;
import ba.unze.edom.server.service.korisnik.RegistracijaService;
import ba.unze.edom.server.service.student.SifraStudenta;
import ba.unze.edom.server.service.student.StudentEvidencijaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Registracija naloga na webu.
 * Da li je student nov ili postojeći odlučuje StudentEvidencijaService
 * (ima svoj test), pa je ovdje zamijenjen Mockito objektom.
 */
@ExtendWith(MockitoExtension.class)
class RegistracijaServiceTest {

    private static final String LOZINKA = "NovaLozinka1!";
    private static final String EMAIL = "merjem@example.com";

    @Mock StudentEvidencijaService studentEvidencija;
    @Mock KorisnikRepository korisnikRepository;
    @Mock UlogaRepository ulogaRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock SifraStudenta sifraStudenta;

    @InjectMocks
    RegistracijaService service;

    private Uloga ulogaStudent;
    private Student student;

    @BeforeEach
    void pripremi() {
        ulogaStudent = new Uloga();
        ulogaStudent.setIdUloga(1);
        ulogaStudent.setNaziv("Student");

        student = new Student();
        student.setIdStudent(42);
        student.setIme("Merjem");
        student.setPrezime("Grizić");
    }

    private RegistracijaZahtjev zahtjev() {
        return new RegistracijaZahtjev(
                "Merjem", "Grizić", "0101003175007", "320",
                EMAIL, "061111222", "Drugo",
                3, "Zenica, Ulica 1", "Adem", LOZINKA, true);
    }

    /** Uobičajen slučaj: student pronađen ili kreiran, nalog još ne postoji, e-mail slobodan. */
    private void sveIspravno() {
        when(studentEvidencija.nadjiIliKreiraj(any())).thenReturn(student);
        when(korisnikRepository.findByStudent_IdStudent(42)).thenReturn(Optional.empty());
        when(korisnikRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(ulogaRepository.findByNaziv("Student")).thenReturn(Optional.of(ulogaStudent));
        when(passwordEncoder.encode(LOZINKA)).thenReturn("$2a$10$hash");
        when(korisnikRepository.save(any(Korisnik.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Nalog se pravi s ulogom Student i vezuje za pronađenog studenta")
    void nalogVezanZaStudenta() {
        sveIspravno();

        Korisnik nalog = service.registruj(zahtjev());

        assertSame(student, nalog.getStudent());
        assertEquals("Student", nalog.getUloga().getNaziv());
        assertEquals(EMAIL, nalog.getEmail());
        assertEquals(EMAIL, nalog.getUsername());
        assertEquals("Merjem", nalog.getIme());
    }

    @Test
    @DisplayName("Podaci iz registracije prosljeđuju se evidenciji studenata")
    void podaciIduUEvidenciju() {
        sveIspravno();

        service.registruj(zahtjev());

        ArgumentCaptor<StudentUnosDTO> c = ArgumentCaptor.forClass(StudentUnosDTO.class);
        verify(studentEvidencija).nadjiIliKreiraj(c.capture());
        assertEquals("0101003175007", c.getValue().jmbg());
        assertEquals(EMAIL, c.getValue().email());
        assertEquals("Drugo", c.getValue().fakultet());
    }

    @Test
    @DisplayName("Studentu se dodjeljuje šifra")
    void dodjeljujeSeSifra() {
        sveIspravno();

        service.registruj(zahtjev());

        verify(sifraStudenta).dodijeli(student);
    }

    @Test
    @DisplayName("Student koji već ima nalog dobija grešku i ne pravi se drugi nalog")
    void vecImaNalog() {
        when(studentEvidencija.nadjiIliKreiraj(any())).thenReturn(student);
        when(korisnikRepository.findByStudent_IdStudent(42)).thenReturn(Optional.of(new Korisnik()));

        var greska = assertThrows(RegistracijaException.class, () -> service.registruj(zahtjev()));

        assertTrue(greska.getMessage().contains("nalog"));
        verify(korisnikRepository, never()).save(any());
    }

    @Test
    @DisplayName("Zauzet e-mail se odbija")
    void zauzetEmail() {
        when(studentEvidencija.nadjiIliKreiraj(any())).thenReturn(student);
        when(korisnikRepository.findByStudent_IdStudent(42)).thenReturn(Optional.empty());
        when(korisnikRepository.findByEmail(EMAIL)).thenReturn(Optional.of(new Korisnik()));

        var greska = assertThrows(RegistracijaException.class, () -> service.registruj(zahtjev()));

        assertTrue(greska.getMessage().contains("e-mail"));
        verify(korisnikRepository, never()).save(any());
    }

    @Test
    @DisplayName("Greška iz evidencije (npr. nepoznat fakultet) zaustavlja registraciju")
    void greskaIzEvidencije() {
        when(studentEvidencija.nadjiIliKreiraj(any()))
                .thenThrow(new RegistracijaException("Odaberite fakultet iz liste."));

        assertThrows(RegistracijaException.class, () -> service.registruj(zahtjev()));

        verifyNoInteractions(sifraStudenta);
        verify(korisnikRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lozinka se nikad ne čuva u čitljivom obliku")
    void lozinkaSeHesira() {
        sveIspravno();

        Korisnik nalog = service.registruj(zahtjev());

        assertNotEquals(LOZINKA, nalog.getPasswordHash());
        assertEquals("$2a$10$hash", nalog.getPasswordHash());
        verify(passwordEncoder).encode(LOZINKA);
    }
}