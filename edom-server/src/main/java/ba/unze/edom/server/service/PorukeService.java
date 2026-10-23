package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.PorukaDTO;
import ba.unze.edom.server.dto.PorukaStudenta;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Kontakt forma: poruka ide mailom domu i čeka u fajlu dok je admin ne pročita. */
@Service
public class PorukeService {

    private static final Logger log = LoggerFactory.getLogger(PorukeService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");
    private static final int MAX_NEPROCITANIH = 5;

    private final StudentRepository studentRepository;
    private final EmailService emailService;
    private final Path fajl;
    private final String adresaDoma;

    private List<PorukaStudenta> poruke;   // učita se iz fajla pri prvom pozivu

    public PorukeService(StudentRepository studentRepository,
                         EmailService emailService,
                         @Value("${edom.poruke.fajl:data/poruke.ser}") String putanja,
                         @Value("${edom.mail.dom:${edom.mail.od}}") String adresaDoma) {
        this.studentRepository = studentRepository;
        this.emailService = emailService;
        this.fajl = Path.of(putanja);
        this.adresaDoma = adresaDoma;
    }

    // ---------- student ----------

    public void posalji(Integer idStudenta, String naslov, String tekst) {
        String n = naslov == null ? "" : naslov.trim();
        String t = tekst == null ? "" : tekst.trim();
        if (t.isEmpty()) throw new PrijavaException("Upišite poruku.");
        if (n.length() > 100) throw new PrijavaException("Naslov može imati najviše 100 znakova.");
        if (t.length() > 2000) throw new PrijavaException("Poruka može imati najviše 2000 znakova.");

        Student s = studentRepository.findById(idStudenta)
                .orElseThrow(() -> new PrijavaException("Student ne postoji."));

        PorukaStudenta p = new PorukaStudenta(
                UUID.randomUUID().toString(),
                s.getIdStudent(),
                s.getIme() + " " + s.getPrezime(),
                s.getEmail(),
                SifraStudenta.format(s.getSifraBroj(), null),
                n.isEmpty() ? "Poruka studenta" : n,
                t,
                LocalDateTime.now(ZONA));

        synchronized (this) {
            ucitaj();
            long nepročitane = poruke.stream().filter(x -> x.idStudent().equals(s.getIdStudent())).count();
            if (nepročitane >= MAX_NEPROCITANIH) {
                throw new PrijavaException("Već imate " + MAX_NEPROCITANIH
                        + " poruka koje administracija još nije pročitala. Pokušajte kasnije.");
            }
            poruke.add(p);
            snimi();
        }

        // mail domu; ako ne uspije, poruka je ipak sačuvana i vidi se pod zvonom
        try {
            emailService.posaljiPorukuDomu(adresaDoma, p.email(), p.ime(), p.sifra(), p.naslov(), p.tekst());
        } catch (Exception e) {
            log.warn("Poruka studenta nije poslana mailom na {}: {}", adresaDoma, e.getMessage());
        }
    }

    // ---------- admin ----------

    public synchronized List<PorukaStudenta> nepročitane() {
        ucitaj();
        List<PorukaStudenta> kopija = new ArrayList<>(poruke);
        kopija.sort(Comparator.comparing(PorukaStudenta::vrijeme).reversed());
        return kopija;
    }

    public List<PorukaDTO> zaAdmina() {
        return nepročitane().stream().map(p -> new PorukaDTO(
                p.id(), p.ime(), p.email(), p.sifra(), p.naslov(), p.tekst(), p.vrijeme().format(FMT))).toList();
    }

    public synchronized void procitano(String id) {
        ucitaj();
        if (poruke.removeIf(p -> p.id().equals(id))) snimi();
    }

    // ---------- fajl ----------

    @SuppressWarnings("unchecked")
    private void ucitaj() {
        if (poruke != null) return;
        poruke = new ArrayList<>();
        if (!Files.exists(fajl)) return;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(fajl))) {
            poruke.addAll((List<PorukaStudenta>) in.readObject());
        } catch (Exception e) {
            log.warn("Poruke se ne mogu pročitati iz {}: {}", fajl.toAbsolutePath(), e.getMessage());
        }
    }

    private void snimi() {
        try {
            if (fajl.getParent() != null) Files.createDirectories(fajl.getParent());
            Path privremeni = fajl.resolveSibling(fajl.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(privremeni))) {
                out.writeObject(new ArrayList<>(poruke));
            }
            Files.move(privremeni, fajl, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("Poruke nisu snimljene u {}", fajl.toAbsolutePath(), e);
            throw new PrijavaException("Poruka trenutno ne može biti sačuvana. Pokušajte kasnije.");
        }
    }
}