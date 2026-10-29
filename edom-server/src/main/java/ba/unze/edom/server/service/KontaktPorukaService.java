package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.KontaktForma;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** Poruke s javne stranice Kontakt → e-mail Studentskom centru. */
@Service
@RequiredArgsConstructor
public class KontaktPorukaService {

    private static final Logger log = LoggerFactory.getLogger(KontaktPorukaService.class);

    private static final List<String> TEME = List.of(
            "Konkurs i prijava", "Dokumenti", "Rang lista i useljenje", "Smještaj i ishrana", "Ostalo");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");
    private static final int MAX_PO_ADRESI = 3;
    private static final Duration PROZOR = Duration.ofMinutes(10);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm");
    private static final String PRIMALAC_PODRAZUMIJEVANI = "tehnickisekretar.sc@unze.ba";

    private final JavaMailSender mailSender;
    private final WebTekstoviService webTekstovi;

    @Value("${spring.mail.username:}")
    private String posiljalac;

    /** Za testiranje: u application.properties stavi svoj e-mail, da poruke ne idu u SC. */
    @Value("${edom.kontakt.primalac:}")
    private String primalacIzPostavki;

    private final Map<String, Deque<Instant>> slanja = new ConcurrentHashMap<>();

    public static class KontaktGreska extends RuntimeException {
        public KontaktGreska(String poruka) { super(poruka); }
    }

    public List<String> teme() {
        return TEME;
    }

    public void posalji(KontaktForma f, String ipAdresa) {
        if (f == null) throw new KontaktGreska("Popunite formu.");

        // bot je popunio skriveno polje: pravimo se da je poslano, ništa ne šaljemo
        if (f.web() != null && !f.web().isBlank()) {
            log.info("Kontakt forma: odbačena poruka bota ({}).", ipAdresa);
            return;
        }

        String ime = jednaLinija(f.ime());
        String email = jednaLinija(f.email());
        String tema = TEME.contains(f.tema()) ? f.tema() : "Ostalo";
        String poruka = f.poruka() == null ? "" : f.poruka().replace("\r", "").strip();

        if (ime.length() < 2 || ime.length() > 80) throw new KontaktGreska("Upišite ime i prezime (2 do 80 znakova).");
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) throw new KontaktGreska("E-mail adresa nije ispravna.");
        if (poruka.length() < 10) throw new KontaktGreska("Poruka je prekratka (najmanje 10 znakova).");
        if (poruka.length() > 2000) throw new KontaktGreska("Poruka je predugačka (najviše 2000 znakova).");

        provjeriUcestalost(ipAdresa);

        String primalac = primalac();
        SimpleMailMessage m = new SimpleMailMessage();
        if (!posiljalac.isBlank()) m.setFrom(posiljalac);
        m.setTo(primalac);
        m.setReplyTo(email);                      // "Odgovori" ide direktno studentu
        m.setSubject("[E-Dom kontakt] " + tema + " — " + ime);
        m.setText("""
                Nova poruka sa stranice Kontakt (E-Dom).

                Ime i prezime: %s
                E-mail:        %s
                Tema:          %s
                Poslano:       %s

                ---------------------------------------------
                %s
                ---------------------------------------------

                Odgovorite na ovaj e-mail — odgovor ide direktno pošiljaocu.
                """.formatted(ime, email, tema,
                LocalDateTime.now(ZoneId.of("Europe/Sarajevo")).format(FMT), poruka));

        try {
            mailSender.send(m);
        } catch (MailException e) {
            log.error("Kontakt poruka nije poslana na {}", primalac, e);
            throw new KontaktGreska("Poruka trenutno nije poslana. Pokušajte ponovo za nekoliko minuta ili pišite direktno na "
                    + primalac + ".");
        }
    }

    // ---------- pomoćne ----------

    private String primalac() {
        if (primalacIzPostavki != null && !primalacIzPostavki.isBlank()) return primalacIzPostavki.trim();
        String izTeksta = webTekstovi.sve().get("pocetna.kontakt.email");
        return izTeksta != null && EMAIL.matcher(izTeksta.trim()).matches() ? izTeksta.trim() : PRIMALAC_PODRAZUMIJEVANI;
    }

    /** Najviše 3 poruke u 10 minuta s iste adrese. */
    private void provjeriUcestalost(String ip) {
        String kljuc = ip == null ? "?" : ip;
        Instant sad = Instant.now();
        Deque<Instant> lista = slanja.computeIfAbsent(kljuc, k -> new ArrayDeque<>());
        synchronized (lista) {
            while (!lista.isEmpty() && lista.peekFirst().isBefore(sad.minus(PROZOR))) lista.pollFirst();
            if (lista.size() >= MAX_PO_ADRESI) {
                throw new KontaktGreska("Poslali ste više poruka zaredom. Pokušajte ponovo za nekoliko minuta.");
            }
            lista.addLast(sad);
        }
        if (slanja.size() > 5000) slanja.clear();   // da memorija ne raste unedogled
    }

    /** Bez novih redova (zaštita naslova e-maila). */
    private static String jednaLinija(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n\\t]+", " ").strip();
    }
}