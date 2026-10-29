package ba.unze.edom.server.service;

import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Tekstovi javne web stranice koje urednik mijenja direktno na stranici.
 * U fajlu se čuvaju SAMO izmjene; originalni tekst je u HTML šablonu.
 */
@Service
public class WebTekstoviService {

    private static final Path FAJL = Path.of("data", "web-tekstovi.properties");
    private static final Pattern KLJUC = Pattern.compile("[a-z0-9][a-z0-9.\\-]{2,79}");
    private static final int MAX_DUZINA = 4000;

    private final Map<String, String> tekstovi = new ConcurrentHashMap<>();

    @PostConstruct
    void ucitaj() {
        if (!Files.exists(FAJL)) return;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(FAJL, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            throw new IllegalStateException("Ne mogu pročitati " + FAJL.toAbsolutePath(), e);
        }
        p.forEach((k, v) -> tekstovi.put(k.toString(), v.toString()));
    }

    /** Sve izmjene (ključ -> tekst). Ključ kojeg nema = prikazuje se original iz šablona. */
    public Map<String, String> sve() {
        return Collections.unmodifiableMap(tekstovi);
    }

    /** Prazna vrijednost = vrati original (ključ se briše iz fajla). */
    public synchronized void sacuvaj(Map<String, String> izmjene) {
        if (izmjene == null || izmjene.isEmpty()) return;

        for (var e : izmjene.entrySet()) {
            String k = e.getKey();
            if (k == null || !KLJUC.matcher(k).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neispravan ključ teksta: " + k);
            }
            String v = ocisti(e.getValue());
            if (v.length() > MAX_DUZINA) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tekst je predugačak (najviše " + MAX_DUZINA + " znakova).");
            }
        }

        for (var e : izmjene.entrySet()) {
            String v = ocisti(e.getValue());
            if (v.isEmpty()) tekstovi.remove(e.getKey());
            else tekstovi.put(e.getKey(), v);
        }
        snimi();
    }

    private static String ocisti(String s) {
        if (s == null) return "";
        return s.replace("\r", "")
                .replace('\u00A0', ' ')          // tvrdi razmak iz contenteditable
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private void snimi() {
        try {
            Files.createDirectories(FAJL.getParent());
            Path tmp = FAJL.resolveSibling(FAJL.getFileName() + ".tmp");
            Properties p = new Properties();
            p.putAll(new TreeMap<>(tekstovi));
            try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                p.store(w, "Tekstovi javne web stranice (samo izmjene)");
            }
            Files.move(tmp, FAJL, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tekstovi nisu sačuvani.", e);
        }
    }
}