package ba.unze.edom.server.service.student;

import ba.unze.edom.common.dto.student.StudentDetaljiDTO;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.SmjestajRepository;
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

/** Detalji studenta na desktopu i napomena koju administrator šalje studentu mailom. */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentDetaljiService {

    private final PrijavaRepository prijavaRepository;
    private final SmjestajRepository smjestajRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String posiljalac;

    @Transactional(readOnly = true)
    public StudentDetaljiDTO detalji(Integer idPrijave) {
        Prijava p = prijavaRepository.findById(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));
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
                s.getNapomena());
    }

    /** Upiše napomenu kod studenta (zamijeni staru) i pošalje je mailom. Vraća da li je mail poslan. */
    @Transactional
    public boolean posaljiNapomenu(Integer idPrijave, String tekst) {
        Prijava p = prijavaRepository.findById(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));
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
}