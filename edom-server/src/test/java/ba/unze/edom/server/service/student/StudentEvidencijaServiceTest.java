package ba.unze.edom.server.service.student;

import ba.unze.edom.common.dto.student.StudentUnosDTO;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.repository.SocijalniStatusRepository;
import ba.unze.edom.server.repository.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Prepoznavanje "istog čovjeka" po JMBG-u, zajedničko za registraciju na webu
 * i ručni unos na desktopu. Cilj: nijedan student se ne smije pojaviti dvaput.
 */
@ExtendWith(MockitoExtension.class)
class StudentEvidencijaServiceTest {

    /** Ispravni JMBG-ovi (kontrolna cifra tačna): 000 = muški, 500 = ženski. */
    private static final String JMBG_M = "0101003170005";
    private static final String JMBG_Z = "0101003175007";

    @Mock StudentRepository studentRepository;
    @Mock SocijalniStatusRepository socijalniStatusRepository;

    @InjectMocks
    StudentEvidencijaService servis;

    // =====================================================================
    // NOV STUDENT
    // =====================================================================

    @Test
    @DisplayName("Nepoznat JMBG: kreira se i snima nov student")
    void novStudent() {
        when(studentRepository.findByJmbg(JMBG_Z)).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        Student s = servis.nadjiIliKreiraj(unos(JMBG_Z, "320", "Drugo", "nova@example.com"));

        verify(studentRepository).save(s);
        assertEquals(JMBG_Z, s.getJmbg());
        assertEquals("320", s.getBrojIndeksa());
        assertEquals("nova@example.com", s.getEmail());
        assertEquals("Drugo", s.getFakultet());
    }

    @Test
    @DisplayName("Brucoš bez indeksa: prazno polje se čuva kao 'nema indeksa'")
    void brucosBezIndeksa() {
        when(studentRepository.findByJmbg(JMBG_Z)).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        Student s = servis.nadjiIliKreiraj(unos(JMBG_Z, "   ", "Drugo", "nova@example.com"));

        assertNull(s.getBrojIndeksa());
    }

    @Test
    @DisplayName("Spol se određuje iz JMBG-a")
    void spolIzJmbg() {
        when(studentRepository.findByJmbg(any())).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        Student m = servis.nadjiIliKreiraj(unos(JMBG_M, null, "Drugo", "m@example.com"));
        Student z = servis.nadjiIliKreiraj(unos(JMBG_Z, null, "Drugo", "z@example.com"));

        assertNotNull(m.getSpol());
        assertNotNull(z.getSpol());
        assertNotEquals(m.getSpol(), z.getSpol());
    }

    @Test
    @DisplayName("Fakultet koji nije na listi se odbija i ništa se ne snima")
    void nepoznatFakultet() {
        assertThrows(RegistracijaException.class, () ->
                servis.nadjiIliKreiraj(unos(JMBG_Z, "320", "Fakultet koji ne postoji", "x@example.com")));

        verify(studentRepository, never()).save(any());
    }

    // =====================================================================
    // POSTOJEĆI STUDENT (isti JMBG)
    // =====================================================================

    @Test
    @DisplayName("Poznat JMBG: vraća se postojeći student, novi se ne pravi")
    void postojeciBezDuplikata() {
        Student postojeci = postojeci();
        when(studentRepository.findByJmbg(JMBG_Z)).thenReturn(Optional.of(postojeci));

        Student s = servis.nadjiIliKreiraj(unos(JMBG_Z, "320", "Drugo", "nova@example.com"));

        assertSame(postojeci, s);
        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Postojećem studentu se ažuriraju kontakt i godina studija")
    void azuriranjeKontakta() {
        Student postojeci = postojeci();
        when(studentRepository.findByJmbg(JMBG_Z)).thenReturn(Optional.of(postojeci));

        servis.nadjiIliKreiraj(unos(JMBG_Z, "320", "Drugo", "nova@example.com"));

        assertEquals("nova@example.com", postojeci.getEmail());
        assertEquals("061999888", postojeci.getTelefon());
        assertEquals(3, postojeci.getGodinaStudija());
    }

    @Test
    @DisplayName("Prazan indeks ne briše indeks koji je sekretar ranije unio")
    void prazanIndeksNeBrise() {
        Student postojeci = postojeci();
        when(studentRepository.findByJmbg(JMBG_Z)).thenReturn(Optional.of(postojeci));

        servis.nadjiIliKreiraj(unos(JMBG_Z, "", "Drugo", "nova@example.com"));

        assertEquals("111", postojeci.getBrojIndeksa());
    }

    @Test
    @DisplayName("Novi indeks zamjenjuje stari (npr. prelazak na drugi fakultet)")
    void noviIndeks() {
        Student postojeci = postojeci();
        when(studentRepository.findByJmbg(JMBG_Z)).thenReturn(Optional.of(postojeci));

        servis.nadjiIliKreiraj(unos(JMBG_Z, "555", "Drugo", "nova@example.com"));

        assertEquals("555", postojeci.getBrojIndeksa());
    }

    @Test
    @DisplayName("'Drugo' ne briše pravi fakultet koji je već upisan")
    void drugoNeBriseFakultet() {
        Student postojeci = postojeci();
        when(studentRepository.findByJmbg(JMBG_Z)).thenReturn(Optional.of(postojeci));

        servis.nadjiIliKreiraj(unos(JMBG_Z, "320", "Drugo", "nova@example.com"));

        assertEquals("Upisani fakultet", postojeci.getFakultet());
    }

    @Test
    @DisplayName("Registracija bez broja lične karte ne briše postojeći broj")
    void licnaKartaSeNeBrise() {
        Student postojeci = postojeci();
        postojeci.setBrojLicneKarte("1E2345678");
        when(studentRepository.findByJmbg(JMBG_Z)).thenReturn(Optional.of(postojeci));

        servis.nadjiIliKreiraj(unos(JMBG_Z, "320", "Drugo", "nova@example.com"));

        assertEquals("1E2345678", postojeci.getBrojLicneKarte());
    }

    @Test
    @DisplayName("Broj lične karte se snima velikim slovima, bez razmaka")
    void licnaKartaVelikimSlovima() {
        Student postojeci = postojeci();
        when(studentRepository.findByJmbg(JMBG_Z)).thenReturn(Optional.of(postojeci));

        servis.nadjiIliKreiraj(new StudentUnosDTO(
                "Merjem", "Grizić", JMBG_Z, "320", "nova@example.com", "061999888",
                "Drugo", 3, "Zenica", "Adem", null,
                "  1e2345678 ", null, null, null));

        assertEquals("1E2345678", postojeci.getBrojLicneKarte());
    }

    // ---------- pomoćne ----------

    /** Isti redoslijed polja kao u RegistracijaService. */
    private static StudentUnosDTO unos(String jmbg, String indeks, String fakultet, String email) {
        return new StudentUnosDTO(
                "Merjem", "Grizić", jmbg, indeks, email, "061999888",
                fakultet, 3, "Zenica", "Adem", null,
                null, null, null, null);
    }

    private static Student postojeci() {
        Student s = new Student();
        s.setIdStudent(42);
        s.setIme("Merjem");
        s.setPrezime("Grizić");
        s.setJmbg(JMBG_Z);
        s.setBrojIndeksa("111");
        s.setFakultet("Upisani fakultet");
        s.setEmail("staro@example.com");
        s.setTelefon("061000000");
        s.setGodinaStudija(2);
        return s;
    }
}