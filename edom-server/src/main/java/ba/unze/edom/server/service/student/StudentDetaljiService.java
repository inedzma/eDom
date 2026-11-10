package ba.unze.edom.server.service.student;

import ba.unze.edom.common.dto.student.StudentDetaljiDTO;
import ba.unze.edom.common.dto.student.StudentIzmjenaDTO;
import ba.unze.edom.common.util.Fakulteti;
import ba.unze.edom.common.util.Imena;
import ba.unze.edom.common.util.Jmbg;
import ba.unze.edom.common.util.Opcine;
import ba.unze.edom.server.entity.Korisnik;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Smjestaj;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.KorisnikRepository;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.SmjestajRepository;
import ba.unze.edom.server.repository.StudentRepository;
import ba.unze.edom.server.service.sobe.SobeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Detalji studenta na desktopu, izmjena podataka i napomena koju administrator šalje studentu mailom. */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentDetaljiService {

    private final PrijavaRepository prijavaRepository;
    private final SmjestajRepository smjestajRepository;
    private final StudentRepository studentRepository;
    private final KorisnikRepository korisnikRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String posiljalac;

    @Transactional(readOnly = true)
    public StudentDetaljiDTO detalji(Integer idPrijave) {
        Prijava p = prijava(idPrijave);
        Student s = p.getStudent();

        List<Prijava> poRedu = smjestajRepository.zaRaspored(p.getAkademskaGodina(), SobeService.STATUSI_ZA_RASPORED);
        Integer rang = null;
        for (int i = 0; i < poRedu.size(); i++) {
            if (poRedu.get(i).getIdPrijava().equals(p.getIdPrijava())) { rang = i + 1; break; }
        }

        Optional<Smjestaj> sm = smjestajRepository.aktivniZaPrijavu(p.getIdPrijava());
        Number bodovi = p.getUkupniBodovi();

        return new StudentDetaljiDTO(
                p.getIdPrijava(), s.getIme(), s.getPrezime(), s.getBrojIndeksa(), s.getJmbg(),
                s.getEmail(), s.getTelefon(), s.getFakultet(), p.getGodinaStudija(), s.getAdresa(), s.getImeRoditelja(),
                p.getAkademskaGodina(), p.getStatus() != null ? p.getStatus().getNaziv() : null,
                bodovi != null ? bodovi.doubleValue() : 0, rang,
                sm.map(x -> x.getSoba().getBroj()).orElse(null),
                sm.map(Smjestaj::getStatus).orElse(null),
                s.getNapomena(),
                s.getBrojLicneKarte(), s.getTelefonRoditelja(), s.getKanton(), s.getOpcina());
    }

    /**
     * Administrator mijenja podatke studenta. Ako student ima nalog, ime, prezime i e-mail
     * mijenjaju se i na nalogu (e-mail je ujedno korisničko ime za prijavu).
     */
    @Transactional
    public StudentDetaljiDTO izmijeni(Integer idPrijave, StudentIzmjenaDTO z) {
        Student s = prijava(idPrijave).getStudent();

        // ---- provjere ----
        String jmbg = z.jmbg().trim();
        studentRepository.findByJmbg(jmbg)
                .filter(drugi -> !drugi.getIdStudent().equals(s.getIdStudent()))
                .ifPresent(drugi -> {
                    throw new PrijavaException("JMBG " + jmbg + " već ima drugi student ("
                            + drugi.getIme() + " " + drugi.getPrezime() + ").");
                });

        String fakultet = z.fakultet().trim();
        if (!StudentEvidencijaService.FAKULTET_DRUGO.equals(fakultet) && !Fakulteti.postoji(fakultet)) {
            throw new PrijavaException("Odaberite fakultet iz liste.");
        }

        String kanton = prazanUNull(z.kanton());
        String opcina = prazanUNull(z.opcina());
        if ((kanton == null) != (opcina == null)) {
            throw new PrijavaException("Unesite i kanton i općinu, ili ostavite oboje prazno.");
        }
        if (kanton != null && !Opcine.postoji(kanton, opcina)) {
            throw new PrijavaException("Općina \"" + opcina + "\" ne pripada kantonu \"" + kanton + "\".");
        }

        String email = prazanUNull(z.email());
        Optional<Korisnik> nalog = korisnikRepository.findByStudent_IdStudent(s.getIdStudent());
        if (nalog.isPresent()) {
            Korisnik k = nalog.get();
            if (email == null) {
                throw new PrijavaException("Student ima nalog, pa e-mail ne smije biti prazan (koristi ga za prijavu).");
            }
            if (!email.equalsIgnoreCase(k.getEmail())) {
                korisnikRepository.findByEmail(email).ifPresent(drugi -> {
                    throw new PrijavaException("E-mail " + email + " već koristi drugi nalog.");
                });
                k.setEmail(email);
                k.setUsername(email);
            }
        }

        // ---- upis ----
        s.setIme(Imena.formatiraj(z.ime()));
        s.setPrezime(Imena.formatiraj(z.prezime()));
        s.setImeRoditelja(prazanUNull(z.imeRoditelja()) == null ? null : Imena.formatiraj(z.imeRoditelja()));
        s.setJmbg(jmbg);
        s.setSpol(Jmbg.spol(jmbg));
        s.setBrojIndeksa(prazanUNull(z.brojIndeksa()));
        s.setFakultet(fakultet);
        s.setEmail(email);
        s.setTelefon(prazanUNull(z.telefon()));
        s.setAdresa(prazanUNull(z.adresa()));
        String lk = prazanUNull(z.brojLicneKarte());
        s.setBrojLicneKarte(lk == null ? null : lk.toUpperCase());
        s.setTelefonRoditelja(prazanUNull(z.telefonRoditelja()));
        s.setKanton(kanton);
        s.setOpcina(opcina);

        nalog.ifPresent(k -> {
            k.setIme(s.getIme());
            k.setPrezime(s.getPrezime());
        });

        return detalji(idPrijave);
    }

    /** Upiše napomenu kod studenta (zamijeni staru) i pošalje je mailom. Vraća da li je mail poslan. */
    @Transactional
    public boolean posaljiNapomenu(Integer idPrijave, String tekst) {
        Prijava p = prijava(idPrijave);
        if (tekst == null || tekst.isBlank()) throw new PrijavaException("Napomena je prazna.");

        Student s = p.getStudent();
        s.setNapomena(tekst.trim());

        if (s.getEmail() == null || s.getEmail().isBlank()) return false;
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            if (!posiljalac.isBlank()) m.setFrom(posiljalac);
            m.setTo(s.getEmail());
            m.setSubject("E-Dom: obavijest Studentskog centra");
            m.setText("Poštovani/a " + s.getIme() + ",\n\n"
                    + s.getNapomena()
                    + "\n\nStudentski centar Univerziteta u Zenici\n"
                    + "(Ova poruka je poslana iz sistema E-Dom.)");
            mailSender.send(m);
            return true;
        } catch (Exception e) {
            log.warn("Napomena za prijavu {} nije poslana mailom: {}", idPrijave, e.getMessage());
            return false;
        }
    }

    // ---------- pomoćne ----------

    private Prijava prijava(Integer idPrijave) {
        return prijavaRepository.findById(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));
    }

    private static String prazanUNull(String t) {
        return t == null || t.isBlank() ? null : t.trim();
    }
}