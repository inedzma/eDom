package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.StudentUnosDTO;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.repository.SocijalniStatusRepository;
import ba.unze.edom.server.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jedino mjesto koje odlučuje je li student "isti čovjek".
 * Koriste ga registracija na webu i ručni unos na desktopu.
 */
@Service
@RequiredArgsConstructor
public class StudentEvidencijaService {

    private final StudentRepository studentRepository;
    private final SocijalniStatusRepository socijalniStatusRepository;

    @Transactional
    public Student nadjiIliKreiraj(StudentUnosDTO z) {

        // 1. JMBG postoji -> isti čovjek; možda novi indeks ili kontakt
        var poJmbg = studentRepository.findByJmbg(z.jmbg());
        if (poJmbg.isPresent()) {
            Student s = poJmbg.get();
            s.setBrojIndeksa(z.brojIndeksa());
            s.setEmail(z.email());
            s.setTelefon(z.telefon());
            s.setGodinaStudija(z.godinaStudija());
            return s;
        }

        // 2. JMBG ne postoji, ali indeks postoji -> sumnjivo, odbij
        if (studentRepository.findByBrojIndeksa(z.brojIndeksa()).isPresent()) {
            throw new RegistracijaException(
                    "Broj indeksa već postoji uz drugi JMBG. Provjerite podatke.");
        }

        // 3. potpuno nov student
        Student s = new Student();
        s.setIme(z.ime());
        s.setPrezime(z.prezime());
        s.setJmbg(z.jmbg());
        s.setBrojIndeksa(z.brojIndeksa());
        s.setEmail(z.email());
        s.setTelefon(z.telefon());
        s.setFakultet(z.fakultet());
        s.setGodinaStudija(z.godinaStudija());
        s.setAdresa(z.adresa());
        s.setImeRoditelja(z.imeRoditelja());
        if (z.idSocijalnogStatusa() != null) {
            socijalniStatusRepository.findById(z.idSocijalnogStatusa())
                    .ifPresent(s::setSocijalniStatus);
        }
        return studentRepository.save(s);
    }
}