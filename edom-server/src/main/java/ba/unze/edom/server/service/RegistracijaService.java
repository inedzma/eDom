package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.RegistracijaZahtjev;
import ba.unze.edom.server.entity.Korisnik;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.repository.KorisnikRepository;
import ba.unze.edom.server.repository.StudentRepository;
import ba.unze.edom.server.repository.UlogaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistracijaService {

    private final StudentRepository studentRepository;
    private final KorisnikRepository korisnikRepository;
    private final UlogaRepository ulogaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Korisnik registruj(RegistracijaZahtjev z) {

        // 1. Provjera po JMBG-u - jaci identifikator
        var poJmbg = studentRepository.findByJmbg(z.jmbg());

        if (poJmbg.isPresent()) {
            Student postojeci = poJmbg.get();

            if (korisnikRepository.findByStudent_IdStudent(postojeci.getIdStudent()).isPresent()) {
                throw new RegistracijaException(
                        "Za ovog studenta vec postoji nalog. Prijavite se ili resetujte lozinku.");
            }

            // isti covjek, mozda novi indeks ili nova adresa
            postojeci.setBrojIndeksa(z.brojIndeksa());
            postojeci.setEmail(z.email());
            postojeci.setTelefon(z.telefon());

            return napraviNalog(postojeci, z);
        }

        // 2. JMBG ne postoji, ali indeks postoji - sumnjivo, odbij
        if (studentRepository.findByBrojIndeksa(z.brojIndeksa()).isPresent()) {
            throw new RegistracijaException(
                    "Podaci se ne podudaraju sa evidencijom. Obratite se administraciji doma.");
        }

        // 3. Potpuno nov student
        Student novi = new Student();
        novi.setIme(z.ime());
        novi.setPrezime(z.prezime());
        novi.setJmbg(z.jmbg());
        novi.setBrojIndeksa(z.brojIndeksa());
        novi.setEmail(z.email());
        novi.setTelefon(z.telefon());
        novi.setFakultet(z.fakultet());
        novi.setGodinaStudija(z.godinaStudija());
        novi.setAdresa(z.adresa());
        novi.setImeRoditelja(z.imeRoditelja());
        studentRepository.save(novi);

        return napraviNalog(novi, z);
    }

    private Korisnik napraviNalog(Student student, RegistracijaZahtjev z) {

        if (korisnikRepository.findByEmail(z.email()).isPresent()) {
            throw new RegistracijaException("Ta e-mail adresa je vec u upotrebi.");
        }

        var ulogaStudent = ulogaRepository.findByNaziv("Student")
                .orElseThrow(() -> new IllegalStateException(
                        "Uloga 'Student' ne postoji u bazi."));

        Korisnik nalog = new Korisnik();
        nalog.setIme(student.getIme());
        nalog.setPrezime(student.getPrezime());
        nalog.setEmail(z.email());
        nalog.setUsername(z.email());          // e-mail sluzi i kao username
        nalog.setPasswordHash(passwordEncoder.encode(z.lozinka()));
        nalog.setUloga(ulogaStudent);
        nalog.setStudent(student);

        return korisnikRepository.save(nalog);
    }
}