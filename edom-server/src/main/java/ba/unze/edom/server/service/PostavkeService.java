package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.PostavkeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Properties;

/**
 * Postavke bez tabele: jedan fajl data/postavke.properties (UTF-8).
 * Ako fajla nema ili neki ključ fali, vrijede podrazumijevane vrijednosti.
 */
@Service
public class PostavkeService {

    private static final Logger log = LoggerFactory.getLogger(PostavkeService.class);
    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final RecordComponent[] POLJA = PostavkeDTO.class.getRecordComponents();

    private final Path fajl;
    private final PostavkeDTO podrazumijevane;
    private PostavkeDTO trenutne;

    private PravilaBodovanja pravila;

    public PostavkeService(@Value("${edom.postavke-fajl:data/postavke.properties}") String putanja,
                           @Value("${edom.rang.kapacitet:220}") int kapacitet,
                           @Value("${edom.rang.udio-brucosa:0.35}") double udioBrucosa,
                           @Value("${edom.ugovor.cijena:165,00}") String cijena,
                           @Value("${edom.ugovor.direktor:}") String direktor,
                           @Value("${edom.ugovor.period-od:01.10.}") String periodOd,
                           @Value("${edom.ugovor.period-do:15.07.}") String periodDo) {
        this.fajl = Path.of(putanja);
        this.podrazumijevane = new PostavkeDTO(
                LocalDate.now(ZONA).getYear(), null, null, null,
                kapacitet, (int) Math.round(udioBrucosa * 100), 20,
                7, "8:30 – 14:00", "150,00",
                "10,00", "1340100000001672", "2204010", "4810011448", "722659",
                direktor, "DIREKTORICA", "Crkvice 50, 72000 Zenica", "032 226 604", "tehnickisekretar.sc@unze.ba",
                "01-01-1-1495/17 od 28.04.2017.", "01-01-1-1549/25 od 14.4.2025.", "09-130-020",
                cijena, periodOd, periodDo,
                PravilaBodovanja.DEF_BRUCOS_MNOZILAC, PravilaBodovanja.DEF_BRUCOS_DODATAK,
                PravilaBodovanja.DEF_PROSJEK_MNOZILAC, PravilaBodovanja.DEF_ISPIT_MNOZILAC,
                PravilaBodovanja.DEF_BONUS, PravilaBodovanja.DEF_KAZNA,
                PravilaBodovanja.DEF_UDALJENOST, PravilaBodovanja.DEF_PRIMANJA,
                null, null);
    }

    // =====================================================================
    //  CITANJE / SNIMANJE
    // =====================================================================

    public synchronized PostavkeDTO trenutne() {
        if (trenutne == null) trenutne = procitaj();
        return trenutne;
    }

    public synchronized PostavkeDTO sacuvaj(PostavkeDTO z) {
        provjeri(z);

        Object[] v = vrijednosti(z);
        v[indeks("izmijenio")] = trenutniAdmin();
        v[indeks("izmijenjeno")] = LocalDateTime.now(ZONA).format(FMT);
        PostavkeDTO nove = napravi(v);

        snimi(nove);
        trenutne = procitaj();   // prazna polja → podrazumijevane vrijednosti
        pravila = null;          // preračunaj pravila bodovanja
        return trenutne;
    }

    // =====================================================================
    //  POMOCNE ZA OSTATAK APLIKACIJE (koristi ih 2. korak)
    // =====================================================================

    public int akademskaGodina() {
        Integer g = trenutne().akademskaGodina();
        return g != null ? g : LocalDate.now(ZONA).getYear();
    }

    public boolean prijaveOtvorene() {
        PostavkeDTO p = trenutne();
        LocalDate danas = LocalDate.now(ZONA);
        return (p.prijaveOd() == null || !danas.isBefore(p.prijaveOd()))
                && (p.prijaveDo() == null || !danas.isAfter(p.prijaveDo()));
    }

    public int kapacitet() {
        return trenutne().kapacitet();
    }

    public double udioBrucosa() {
        return trenutne().udioBrucosaPosto() / 100.0;
    }

    // =====================================================================
    //  INTERNO
    // =====================================================================

    private void provjeri(PostavkeDTO z) {
        if (z.akademskaGodina() == null || z.akademskaGodina() < 2000 || z.akademskaGodina() > 2100) {
            greska("Upišite akademsku godinu kao prvu godinu, npr. 2026 za 2026/2027.");
        }
        if (z.kapacitet() == null || z.kapacitet() < 1) greska("Kapacitet mora biti veći od 0.");
        if (z.udioBrucosaPosto() == null || z.udioBrucosaPosto() < 0 || z.udioBrucosaPosto() > 100) {
            greska("Udio brucoša mora biti između 0 i 100 %.");
        }
        if (z.prijaveOd() != null && z.prijaveDo() != null && z.prijaveOd().isAfter(z.prijaveDo())) {
            greska("Početak prijava je poslije krajnjeg roka.");
        }
        if (z.minUdaljenostKm() != null && z.minUdaljenostKm() < 0) greska("Minimalna udaljenost ne može biti negativna.");
        if (z.rokZalbeDana() != null && z.rokZalbeDana() < 0) greska("Rok za žalbe ne može biti negativan.");
        try {
            PravilaBodovanja.iz(z);
        } catch (IllegalArgumentException e) {
            greska("Pravila bodovanja – " + e.getMessage());
        }
    }

    private PostavkeDTO procitaj() {
        if (!Files.exists(fajl)) return podrazumijevane;

        Properties pr = new Properties();
        try (Reader r = Files.newBufferedReader(fajl, StandardCharsets.UTF_8)) {
            pr.load(r);
        } catch (IOException e) {
            log.warn("Postavke se ne mogu pročitati iz {} – koriste se podrazumijevane.", fajl.toAbsolutePath(), e);
            return podrazumijevane;
        }

        Object[] def = vrijednosti(podrazumijevane);
        Object[] v = new Object[POLJA.length];
        for (int i = 0; i < POLJA.length; i++) {
            String s = pr.getProperty(POLJA[i].getName());
            v[i] = s == null || s.isBlank() ? def[i] : parsiraj(POLJA[i].getType(), s.trim(), def[i]);
        }
        return napravi(v);
    }

    private void snimi(PostavkeDTO p) {
        Properties pr = new Properties();
        Object[] v = vrijednosti(p);
        for (int i = 0; i < POLJA.length; i++) {
            if (v[i] == null) continue;
            String s = v[i].toString().trim();          // LocalDate → 2026-09-30
            if (!s.isEmpty()) pr.setProperty(POLJA[i].getName(), s);
        }
        try {
            if (fajl.getParent() != null) Files.createDirectories(fajl.getParent());
            Path tmp = fajl.resolveSibling(fajl.getFileName() + ".tmp");
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                pr.store(w, "E-Dom postavke (mijenjati preko desktop aplikacije: Postavke)");
            }
            Files.move(tmp, fajl, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("Postavke nisu snimljene u {}", fajl.toAbsolutePath(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Postavke nisu sačuvane: greška pri snimanju na serveru.");
        }
    }

    private static Object parsiraj(Class<?> tip, String s, Object def) {
        try {
            if (tip == Integer.class) return Integer.valueOf(s);
            if (tip == LocalDate.class) return LocalDate.parse(s);
            return s;
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static Object[] vrijednosti(PostavkeDTO p) {
        Object[] v = new Object[POLJA.length];
        try {
            for (int i = 0; i < POLJA.length; i++) {
                Object x = POLJA[i].getAccessor().invoke(p);
                v[i] = x instanceof String s && s.isBlank() ? null : x;
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return v;
    }

    private static PostavkeDTO napravi(Object[] v) {
        try {
            Class<?>[] tipovi = Arrays.stream(POLJA).map(RecordComponent::getType).toArray(Class<?>[]::new);
            return PostavkeDTO.class.getDeclaredConstructor(tipovi).newInstance(v);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int indeks(String ime) {
        for (int i = 0; i < POLJA.length; i++) if (POLJA[i].getName().equals(ime)) return i;
        throw new IllegalArgumentException(ime);
    }

    private static void greska(String poruka) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, poruka);
    }

    private static String trenutniAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : "admin";
    }

    /** Pravila za uspjeh, udaljenost i primanja (koristi BodovanjeService). */
    public synchronized PravilaBodovanja pravila() {
        if (pravila == null) {
            try {
                pravila = PravilaBodovanja.iz(trenutne());
            } catch (IllegalArgumentException e) {
                log.warn("Pravila bodovanja u postavkama nisu ispravna ({}) – koriste se podrazumijevana.", e.getMessage());
                pravila = PravilaBodovanja.iz(podrazumijevane);
            }
        }
        return pravila;
    }

    // =====================================================================
    //  KONKURS ZA WEB (status, rok)
    // =====================================================================

    public record KonkursInfo(String godina, boolean otvoren, String status, String rokDo) {}

    private static final DateTimeFormatter DAN = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    public KonkursInfo konkurs() {
        PostavkeDTO p = trenutne();
        int g = akademskaGodina();
        LocalDate danas = LocalDate.now(ZONA);
        String rok = p.prijaveDo() != null ? p.prijaveDo().format(DAN) : null;

        String status;
        if (p.prijaveOd() != null && danas.isBefore(p.prijaveOd())) {
            status = "Prijave na konkurs počinju " + p.prijaveOd().format(DAN);
        } else if (p.prijaveDo() != null && danas.isAfter(p.prijaveDo())) {
            status = "Konkurs je zatvoren. Rok za prijave istekao je " + rok;
        } else {
            status = rok != null ? "Prijave su otvorene do " + rok : "Prijave su otvorene.";
        }
        return new KonkursInfo(g + "/" + (g + 1), prijaveOtvorene(), status, rok);
    }

    /** Poruka kad student pokuša prijavu van roka. */
    public String porukaZatvoreno() {
        return konkurs().status() + " Za dodatne informacije obratite se Studentskom centru.";
    }
}