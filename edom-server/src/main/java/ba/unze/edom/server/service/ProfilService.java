package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.ProfilDTO;
import ba.unze.edom.server.dto.ProfilIzmjenaDTO;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.repository.KorisnikRepository;
import ba.unze.edom.server.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private final StudentRepository studentRepository;
    private final KorisnikRepository korisnikRepository;

    @Transactional(readOnly = true)
    public ProfilDTO ucitaj(Integer idStudenta) {
        var s = studentRepository.findById(idStudenta)
                .orElseThrow(() -> new IllegalStateException("Student ne postoji"));

        return new ProfilDTO(
                s.getIme(), s.getPrezime(), s.getJmbg(), s.getBrojIndeksa(),
                s.getImeRoditelja(), s.getEmail(), s.getTelefon(),
                s.getAdresa(), s.getFakultet(), s.getGodinaStudija()
        );
    }

    @Transactional
    public void sacuvaj(Integer idKorisnika, Integer idStudenta, ProfilIzmjenaDTO izmjena) {

        var korisnik = korisnikRepository.findById(idKorisnika)
                .orElseThrow(() -> new IllegalStateException("Korisnik ne postoji"));

        // e-mail je ujedno i korisnicko ime, pa mora ostati jedinstven
        if (!korisnik.getEmail().equalsIgnoreCase(izmjena.email())) {
            korisnikRepository.findByEmail(izmjena.email()).ifPresent(k -> {
                throw new RegistracijaException("Ta e-mail adresa je već u upotrebi.");
            });
            korisnik.setEmail(izmjena.email());
            korisnik.setUsername(izmjena.email());
        }

        var student = studentRepository.findById(idStudenta)
                .orElseThrow(() -> new IllegalStateException("Student ne postoji"));

        student.setEmail(izmjena.email());
        student.setTelefon(izmjena.telefon());
        student.setAdresa(izmjena.adresa());
        student.setFakultet(izmjena.fakultet());
        student.setGodinaStudija(izmjena.godinaStudija());
    }
}