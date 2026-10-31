package ba.unze.edom.server.service.javna;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Fotografije za galeriju na stranici Usluge. Čuvaju se u data/galerija,
 * opisi slika su u web-tekstovima (ključ "galerija.<ime bez ekstenzije>").
 */
@Service
@RequiredArgsConstructor
public class GalerijaService {

    public static final Path FOLDER = Path.of("data", "galerija");
    public static final long MAX_BAJTOVA = 8L * 1024 * 1024;
    public static final int MAX_SLIKA = 30;

    /** Imena pravi server (npr. 20261029-151900-01ab3x.jpg), nikad korisnik. */
    private static final Pattern IME = Pattern.compile("\\d{8}-\\d{6}-\\d{2}[a-z0-9]{4}\\.(jpg|png|webp)");
    private static final DateTimeFormatter VRIJEME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");
    private static final String ZNAKOVI = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RND = new SecureRandom();

    private final WebTekstoviService webTekstovi;

    public record Slika(String ime, String url, String kljuc) {}

    /** Slike po redoslijedu dodavanja (ime počinje vremenom). */
    public List<Slika> lista() {
        if (!Files.isDirectory(FOLDER)) return List.of();
        try (Stream<Path> s = Files.list(FOLDER)) {
            return s.map(p -> p.getFileName().toString())
                    .filter(n -> IME.matcher(n).matches())
                    .sorted()
                    .map(n -> new Slika(n, "/galerija/" + n, kljuc(n)))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Sve ili ništa: ako ijedna slika ne valja, ne snima se nijedna. */
    public synchronized int dodaj(List<MultipartFile> fajlovi) {
        List<MultipartFile> prave = fajlovi == null ? List.of()
                : fajlovi.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (prave.isEmpty()) throw greska("Niste izabrali nijednu fotografiju.");

        int ima = lista().size();
        if (ima + prave.size() > MAX_SLIKA) {
            throw greska("Galerija može imati najviše " + MAX_SLIKA + " fotografija (sada ih ima " + ima
                    + "). Prvo obrišite neke.");
        }

        // 1) provjera svih
        List<byte[]> sadrzaj = new ArrayList<>();
        List<String> ekstenzije = new ArrayList<>();
        for (MultipartFile f : prave) {
            String naziv = f.getOriginalFilename() != null ? f.getOriginalFilename() : "fotografija";
            if (f.getSize() > MAX_BAJTOVA) {
                throw greska("\"" + naziv + "\" je veća od 8 MB.");
            }
            byte[] b;
            try {
                b = f.getBytes();
            } catch (IOException e) {
                throw greska("\"" + naziv + "\" se ne može pročitati.");
            }
            String ext = tip(b);
            if (ext == null) {
                throw greska("\"" + naziv + "\" nije fotografija (dozvoljeno: JPG, PNG, WEBP).");
            }
            sadrzaj.add(b);
            ekstenzije.add(ext);
        }

        // 2) snimanje
        try {
            Files.createDirectories(FOLDER);
            String vrijeme = LocalDateTime.now(ZONA).format(VRIJEME);
            for (int i = 0; i < sadrzaj.size(); i++) {
                String ime = vrijeme + "-" + String.format("%02d", i) + nasumicno(4) + "." + ekstenzije.get(i);
                Path tmp = FOLDER.resolve(ime + ".tmp");
                Files.write(tmp, sadrzaj.get(i));
                Files.move(tmp, FOLDER.resolve(ime), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fotografije nisu snimljene na serveru.", e);
        }
        return sadrzaj.size();
    }

    public synchronized void obrisi(String ime) {
        if (ime == null || !IME.matcher(ime).matches()) throw greska("Neispravan naziv fotografije.");
        try {
            if (!Files.deleteIfExists(FOLDER.resolve(ime))) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fotografija više ne postoji.");
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fotografija nije obrisana.", e);
        }
        webTekstovi.sacuvaj(Map.of(kljuc(ime), ""));   // obriši i opis
    }

    // ---------- pomoćne ----------

    static String kljuc(String ime) {
        return "galerija." + ime.substring(0, ime.lastIndexOf('.'));
    }

    /** Vrsta slike po prvim bajtovima, ne po nazivu fajla. */
    private static String tip(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }

    private static String nasumicno(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(ZNAKOVI.charAt(RND.nextInt(ZNAKOVI.length())));
        return sb.toString();
    }

    private static ResponseStatusException greska(String poruka) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, poruka);
    }
}