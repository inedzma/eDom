package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.StudentUnosDTO;
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

    private final StudentEvidencijaService studentEvidencija;
    private final KorisnikRepository korisnikRepository;
    private final UlogaRepository ulogaRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public Korisnik registruj(RegistracijaZahtjev z) {
        Student student = studentEvidencija.nadjiIliKreiraj(new StudentUnosDTO(
                z.ime(), z.prezime(), z.jmbg(), z.brojIndeksa(), z.email(), z.telefon(),
                z.fakultet(), z.godinaStudija(), z.adresa(), z.imeRoditelja(), null));

        if (korisnikRepository.findByStudent_IdStudent(student.getIdStudent()).isPresent()) {
            throw new RegistracijaException(
                    "Za ovog studenta već postoji nalog. Prijavite se ili resetujte lozinku.");
        }

        return napraviNalog(student, z.email(), passwordEncoder.encode(z.lozinka()));
    }


    @Transactional
    public Korisnik napraviNalog(Student student, String email, String hashLozinke) {

        if (korisnikRepository.findByEmail(email).isPresent()) {
            throw new RegistracijaException("Ta e-mail adresa je već u upotrebi.");
        }

        var ulogaStudent = ulogaRepository.findByNaziv("Student")
                .orElseThrow(() -> new IllegalStateException("Uloga 'Student' ne postoji u bazi."));

        Korisnik nalog = new Korisnik();
        nalog.setIme(student.getIme());
        nalog.setPrezime(student.getPrezime());
        nalog.setEmail(email);
        nalog.setUsername(email);
        nalog.setPasswordHash(hashLozinke);
        nalog.setUloga(ulogaStudent);
        nalog.setStudent(student);

        return korisnikRepository.save(nalog);
    }
}