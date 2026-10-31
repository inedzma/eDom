package ba.unze.edom.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/** Javni dokumenti (pravilnici, obrasci, konkurs) – fajlovi u data/dokumenti, spisak u spisak.ser. */
@Service
public class JavniDokumentiService {

    private static final Logger log = LoggerFactory.getLogger(JavniDokumentiService.class);

    public static final Path FOLDER = Path.of("data", "dokumenti");
    private static final Path SPISAK = FOLDER.resolve("spisak.ser");
    public static final long MAX_BAJTOVA = 10L * 1024 * 1024;
    private static final int MAX_DOKUMENATA = 100;
    private static final Pattern FAJL = Pattern.compile("[a-f0-9]{12}\\.(pdf|doc|docx|xlsx)");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    /** Naziv kategorije -> sidro na stranici (#konkurs …). Redoslijed = redoslijed na stranici. */
    public static final Map<String, String> KATEGORIJE = new LinkedHashMap<>();
    static {
        KATEGORIJE.put("Konkurs i prijava", "konkurs");
        KATEGORIJE.put("Pravilnici", "pravilnici");
        KATEGORIJE.put("Uplate i obrasci", "uplate");
        KATEGORIJE.put("Ostalo", "ostalo");
    }

    public record Dokument(String id, String naslov, String kategorija, String fajl, long velicina, LocalDate datum)
            implements Serializable {
        public String ekstenzija() { return fajl.substring(fajl.lastIndexOf('.') + 1); }
        public String url() { return "/dokumenti/fajl/" + fajl; }
        public String datumTekst() { return datum == null ? "" : datum.format(FMT); }
        public String velicinaTekst() {
            if (velicina < 1024 * 1024) return Math.max(1, velicina / 1024) + " KB";
            return String.format(Locale.ROOT, "%.1f MB", velicina / 1024.0 / 1024.0).replace('.', ',');
        }
    }

    public record Grupa(String naziv, String sidro, List<Dokument> dokumenti) {}

    private List<Dokument> dokumenti;

    // ---------- čitanje ----------

    public synchronized List<Grupa> grupe() {
        ucitaj();
        List<Grupa> out = new ArrayList<>();
        KATEGORIJE.forEach((naziv, sidro) -> {
            List<Dokument> l = dokumenti.stream()
                    .filter(d -> d.kategorija().equals(naziv))
                    .sorted(Comparator.comparing(Dokument::datum, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            if (!l.isEmpty()) out.add(new Grupa(naziv, sidro, l));
        });
        return out;
    }

    public synchronized int ukupno() {
        ucitaj();
        return dokumenti.size();
    }

    public List<String> kategorije() {
        return List.copyOf(KATEGORIJE.keySet());
    }

    public synchronized Dokument poFajlu(String fajl) {
        ucitaj();
        if (fajl == null || !FAJL.matcher(fajl).matches()) throw nema();
        return dokumenti.stream().filter(d -> d.fajl().equals(fajl)).findFirst().orElseThrow(this::nema);
    }

    public Path putanja(Dokument d) {
        return FOLDER.resolve(d.fajl());
    }

    /** "Pravilnik o unutrašnjem životu.pdf" – ime pod kojim se fajl preuzima. */
    public String nazivZaPreuzimanje(Dokument d) {
        String s = d.naslov().replaceAll("[\\\\/:*?\"<>|\\r\\n]+", " ").trim();
        if (s.length() > 120) s = s.substring(0, 120).trim();
        return (s.isEmpty() ? "dokument" : s) + "." + d.ekstenzija();
    }

    // ---------- admin ----------

    public synchronized void dodaj(String naslov, String kategorija, LocalDate datum, MultipartFile f) {
        ucitaj();
        String n = provjeriNaslov(naslov);
        String k = provjeriKategoriju(kategorija);
        if (dokumenti.size() >= MAX_DOKUMENATA) throw greska("Najviše " + MAX_DOKUMENATA + " dokumenata. Prvo obrišite stare.");
        if (f == null || f.isEmpty()) throw greska("Izaberite fajl.");
        if (f.getSize() > MAX_BAJTOVA) throw greska("Fajl je veći od 10 MB.");

        byte[] b;
        try {
            b = f.getBytes();
        } catch (IOException e) {
            throw greska("Fajl se ne može pročitati.");
        }
        String ext = tip(b, f.getOriginalFilename());
        if (ext == null) throw greska("Dozvoljeni su samo PDF, Word (DOC, DOCX) i Excel (XLSX) fajlovi.");

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String fajl = id + "." + ext;
        try {
            Files.createDirectories(FOLDER);
            Path tmp = FOLDER.resolve(fajl + ".tmp");
            Files.write(tmp, b);
            Files.move(tmp, FOLDER.resolve(fajl), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Dokument nije snimljen na serveru.", e);
        }

        dokumenti.add(new Dokument(id, n, k, fajl, b.length,
                datum != null ? datum : LocalDate.now(ZoneId.of("Europe/Sarajevo"))));
        snimi();
    }

    public synchronized void izmijeni(String id, String naslov, String kategorija, LocalDate datum) {
        ucitaj();
        String n = provjeriNaslov(naslov);
        String k = provjeriKategoriju(kategorija);
        for (int i = 0; i < dokumenti.size(); i++) {
            Dokument d = dokumenti.get(i);
            if (d.id().equals(id)) {
                dokumenti.set(i, new Dokument(d.id(), n, k, d.fajl(), d.velicina(), datum != null ? datum : d.datum()));
                snimi();
                return;
            }
        }
        throw nema();
    }

    public synchronized void obrisi(String id) {
        ucitaj();
        Dokument d = dokumenti.stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow(this::nema);
        dokumenti.remove(d);
        snimi();
        try {
            Files.deleteIfExists(putanja(d));
        } catch (IOException e) {
            log.warn("Fajl {} nije obrisan s diska: {}", d.fajl(), e.getMessage());
        }
    }

    // ---------- pomoćne ----------

    private String provjeriNaslov(String naslov) {
        String n = naslov == null ? "" : naslov.replaceAll("\\s+", " ").trim();
        if (n.length() < 3) throw greska("Upišite naslov dokumenta.");
        if (n.length() > 250) throw greska("Naslov može imati najviše 250 znakova.");
        return n;
    }

    private String provjeriKategoriju(String k) {
        if (k == null || !KATEGORIJE.containsKey(k)) throw greska("Izaberite kategoriju.");
        return k;
    }

    /** Vrsta po prvim bajtovima; DOCX i XLSX su oba ZIP, pa se tu gleda i ekstenzija. */
    private static String tip(byte[] b, String originalnoIme) {
        String ime = originalnoIme == null ? "" : originalnoIme.toLowerCase(Locale.ROOT);
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return "pdf";
        if (b.length >= 4 && (b[0] & 0xFF) == 0xD0 && (b[1] & 0xFF) == 0xCF && (b[2] & 0xFF) == 0x11 && (b[3] & 0xFF) == 0xE0
                && ime.endsWith(".doc")) return "doc";
        if (b.length >= 4 && b[0] == 'P' && b[1] == 'K' && b[2] == 3 && b[3] == 4) {
            if (ime.endsWith(".docx")) return "docx";
            if (ime.endsWith(".xlsx")) return "xlsx";
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void ucitaj() {
        if (dokumenti != null) return;
        dokumenti = new ArrayList<>();
        if (!Files.exists(SPISAK)) return;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(SPISAK))) {
            dokumenti.addAll((List<Dokument>) in.readObject());
        } catch (Exception e) {
            log.warn("Spisak dokumenata se ne može pročitati ({}).", e.getMessage());
        }
    }

    private void snimi() {
        try {
            Files.createDirectories(FOLDER);
            Path tmp = SPISAK.resolveSibling("spisak.ser.tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(new ArrayList<>(dokumenti));
            }
            Files.move(tmp, SPISAK, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Spisak dokumenata nije snimljen.", e);
        }
    }

    private ResponseStatusException nema() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument ne postoji.");
    }

    private static ResponseStatusException greska(String poruka) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, poruka);
    }
}