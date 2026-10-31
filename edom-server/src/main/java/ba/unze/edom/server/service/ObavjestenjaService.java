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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/** Obavještenja i aktuelnosti – spisak u data/obavjestenja/spisak.ser, prilozi u istom folderu. */
@Service
public class ObavjestenjaService {

    private static final Logger log = LoggerFactory.getLogger(ObavjestenjaService.class);

    public static final Path FOLDER = Path.of("data", "obavjestenja");
    private static final Path SPISAK = FOLDER.resolve("spisak.ser");
    public static final List<String> VRSTE = List.of("Obavještenje", "Aktuelnost");
    private static final long MAX_BAJTOVA = 10L * 1024 * 1024;
    private static final int MAX_OBAVJESTENJA = 300;
    private static final Pattern FAJL = Pattern.compile("[a-f0-9]{12}\\.(pdf|jpg|png|webp)");
    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private static final String[] MJESECI = {"jan", "feb", "mar", "apr", "maj", "jun", "jul", "avg", "sep", "okt", "nov", "dec"};

    public record Obavjestenje(String id, String naslov, String tekst, String vrsta, LocalDate datum, boolean vazno,
                               String prilog, String prilogNaziv, LocalDateTime dodano) implements Serializable {
        public boolean imaPrilog() { return prilog != null; }
        public boolean prilogSlika() { return prilog != null && !prilog.endsWith(".pdf"); }
        public String prilogUrl() { return prilog == null ? null : "/obavjestenja/prilog/" + prilog; }
        public String dan() { return String.valueOf(datum.getDayOfMonth()); }
        public String mjesec() { return MJESECI[datum.getMonthValue() - 1]; }
        public String godina() { return String.valueOf(datum.getYear()); }
        public String datumTekst() { return datum.format(FMT); }
        public String filter() { return "Aktuelnost".equals(vrsta) ? "aktuelnost" : "obavjestenje"; }
    }

    private List<Obavjestenje> lista;

    // ---------- čitanje ----------

    /** Važna prva, zatim najnovija. */
    public synchronized List<Obavjestenje> sva() {
        ucitaj();
        return lista.stream()
                .sorted(Comparator.comparing(Obavjestenje::vazno).reversed()
                        .thenComparing(Obavjestenje::datum, Comparator.reverseOrder())
                        .thenComparing(Obavjestenje::dodano, Comparator.reverseOrder()))
                .toList();
    }

    public List<Obavjestenje> zadnja(int n) {
        return sva().stream().limit(n).toList();
    }

    public List<String> vrste() {
        return VRSTE;
    }

    public synchronized Obavjestenje poPrilogu(String fajl) {
        ucitaj();
        if (fajl == null || !FAJL.matcher(fajl).matches()) throw nema();
        return lista.stream().filter(o -> fajl.equals(o.prilog())).findFirst().orElseThrow(this::nema);
    }

    public Path putanja(String fajl) {
        return FOLDER.resolve(fajl);
    }

    // ---------- admin ----------

    public synchronized void dodaj(String naslov, String tekst, String vrsta, LocalDate datum, boolean vazno,
                                   MultipartFile prilog) {
        ucitaj();
        if (lista.size() >= MAX_OBAVJESTENJA) throw greska("Previše obavještenja. Obrišite stara.");
        String[] p = snimiPrilog(prilog);
        lista.add(new Obavjestenje(noviId(), naslov(naslov), tekst(tekst), vrsta(vrsta),
                datum != null ? datum : LocalDate.now(ZONA), vazno,
                p == null ? null : p[0], p == null ? null : p[1], LocalDateTime.now(ZONA)));
        snimi();
    }

    public synchronized void izmijeni(String id, String naslov, String tekst, String vrsta, LocalDate datum,
                                      boolean vazno, MultipartFile noviPrilog, boolean ukloniPrilog) {
        ucitaj();
        for (int i = 0; i < lista.size(); i++) {
            Obavjestenje o = lista.get(i);
            if (!o.id().equals(id)) continue;

            String prilog = o.prilog(), prilogNaziv = o.prilogNaziv();
            String[] novi = snimiPrilog(noviPrilog);
            if (novi != null || ukloniPrilog) {
                obrisiFajl(prilog);
                prilog = novi == null ? null : novi[0];
                prilogNaziv = novi == null ? null : novi[1];
            }
            lista.set(i, new Obavjestenje(o.id(), naslov(naslov), tekst(tekst), vrsta(vrsta),
                    datum != null ? datum : o.datum(), vazno, prilog, prilogNaziv, o.dodano()));
            snimi();
            return;
        }
        throw nema();
    }

    public synchronized void obrisi(String id) {
        ucitaj();
        Obavjestenje o = lista.stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow(this::nema);
        lista.remove(o);
        snimi();
        obrisiFajl(o.prilog());
    }

    // ---------- pomoćne ----------

    private static String naslov(String s) {
        String n = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        if (n.length() < 3) throw greska("Upišite naslov.");
        if (n.length() > 200) throw greska("Naslov može imati najviše 200 znakova.");
        return n;
    }

    private static String tekst(String s) {
        String t = s == null ? "" : s.replace("\r", "").replaceAll("\\n{3,}", "\n\n").strip();
        if (t.length() > 5000) throw greska("Tekst može imati najviše 5000 znakova.");
        return t;
    }

    private static String vrsta(String v) {
        return VRSTE.contains(v) ? v : VRSTE.get(0);
    }

    /** null = nema priloga; inače [ime fajla na disku, originalni naziv za prikaz]. */
    private String[] snimiPrilog(MultipartFile f) {
        if (f == null || f.isEmpty()) return null;
        if (f.getSize() > MAX_BAJTOVA) throw greska("Prilog je veći od 10 MB.");
        byte[] b;
        try {
            b = f.getBytes();
        } catch (IOException e) {
            throw greska("Prilog se ne može pročitati.");
        }
        String ext = tip(b);
        if (ext == null) throw greska("Prilog mora biti PDF ili slika (JPG, PNG, WEBP).");

        String fajl = noviId() + "." + ext;
        try {
            Files.createDirectories(FOLDER);
            Path tmp = FOLDER.resolve(fajl + ".tmp");
            Files.write(tmp, b);
            Files.move(tmp, FOLDER.resolve(fajl), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Prilog nije snimljen.", e);
        }
        String naziv = f.getOriginalFilename() == null ? "prilog." + ext
                : f.getOriginalFilename().replaceAll("[\\\\/\\r\\n]+", " ").trim();
        if (naziv.length() > 120) naziv = naziv.substring(0, 120);
        return new String[]{fajl, naziv};
    }

    private void obrisiFajl(String fajl) {
        if (fajl == null) return;
        try {
            Files.deleteIfExists(FOLDER.resolve(fajl));
        } catch (IOException e) {
            log.warn("Prilog {} nije obrisan: {}", fajl, e.getMessage());
        }
    }

    private static String tip(byte[] b) {
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return "pdf";
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        return null;
    }

    private static String noviId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @SuppressWarnings("unchecked")
    private void ucitaj() {
        if (lista != null) return;
        lista = new ArrayList<>();
        if (!Files.exists(SPISAK)) return;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(SPISAK))) {
            lista.addAll((List<Obavjestenje>) in.readObject());
        } catch (Exception e) {
            log.warn("Obavještenja se ne mogu pročitati ({}).", e.getMessage());
        }
    }

    private void snimi() {
        try {
            Files.createDirectories(FOLDER);
            Path tmp = SPISAK.resolveSibling("spisak.ser.tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(new ArrayList<>(lista));
            }
            Files.move(tmp, SPISAK, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Obavještenja nisu snimljena.", e);
        }
    }

    private ResponseStatusException nema() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Obavještenje ne postoji.");
    }

    private static ResponseStatusException greska(String poruka) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, poruka);
    }
}