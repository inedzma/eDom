package ba.unze.edom.server.service.student;

import ba.unze.edom.common.dto.student.StudentUnosDTO;
import ba.unze.edom.common.util.Fakulteti;
import ba.unze.edom.common.util.Imena;
import ba.unze.edom.common.util.Jmbg;
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

    /** Brucoš koji još ne zna na koji će fakultet biti upisan. */
    public static final String FAKULTET_DRUGO = "Drugo";

    private final StudentRepository studentRepository;
    private final SocijalniStatusRepository socijalniStatusRepository;

    @Transactional
    public Student nadjiIliKreiraj(StudentUnosDTO z) {

        boolean drugo = FAKULTET_DRUGO.equals(z.fakultet());
        if (!drugo && !Fakulteti.postoji(z.fakultet())) {
            throw new RegistracijaException("Odaberite fakultet iz liste.");
        }

        // brucoš možda još nema indeks: prazno polje = nema indeksa
        String indeks = prazanUNull(z.brojIndeksa());

        // 1. JMBG postoji -> isti čovjek; možda novi indeks, fakultet ili kontakt
        //    (broj indeksa NIJE jedinstven: svaki fakultet i smjer ima svoj niz od 1)
        var poJmbg = studentRepository.findByJmbg(z.jmbg());
        if (poJmbg.isPresent()) {
            Student s = poJmbg.get();
            // prazno ne briše indeks koji je sekretar ranije unio
            if (indeks != null) s.setBrojIndeksa(indeks);
            // "Drugo" ne briše pravi fakultet koji je već upisan
            if (!drugo || s.getFakultet() == null) s.setFakultet(z.fakultet());
            s.setEmail(z.email());
            s.setTelefon(z.telefon());
            s.setGodinaStudija(z.godinaStudija());
            prijavniPodaci(s, z);
            return s;
        }

        // 2. potpuno nov student
        Student s = new Student();
        s.setIme(Imena.formatiraj(z.ime()));
        s.setPrezime(Imena.formatiraj(z.prezime()));
        s.setJmbg(z.jmbg());
        s.setBrojIndeksa(indeks);
        s.setEmail(z.email());
        s.setTelefon(z.telefon());
        s.setFakultet(z.fakultet());
        s.setGodinaStudija(z.godinaStudija());
        s.setAdresa(z.adresa());
        s.setImeRoditelja(Imena.formatiraj(z.imeRoditelja()));
        if (z.idSocijalnogStatusa() != null) {
            socijalniStatusRepository.findById(z.idSocijalnogStatusa())
                    .ifPresent(s::setSocijalniStatus);
        }
        prijavniPodaci(s, z);
        return studentRepository.save(s);
    }

    /**
     * Podaci koje traži prijava (lična karta, telefon roditelja, prebivalište) i spol iz JMBG-a.
     * Registracija ih ne šalje (null), pa se postojeće vrijednosti ne brišu.
     */
    private void prijavniPodaci(Student s, StudentUnosDTO z) {
        if (z.brojLicneKarte() != null && !z.brojLicneKarte().isBlank()) {
            s.setBrojLicneKarte(z.brojLicneKarte().trim().toUpperCase());
        }
        if (z.telefonRoditelja() != null && !z.telefonRoditelja().isBlank()) {
            s.setTelefonRoditelja(z.telefonRoditelja().trim());
        }
        if (z.kanton() != null && z.opcina() != null) {
            s.setKanton(z.kanton());
            s.setOpcina(z.opcina());
        }
        s.setSpol(Jmbg.spol(s.getJmbg()));
    }

    private static String prazanUNull(String t) {
        return t == null || t.isBlank() ? null : t.trim();
    }
}