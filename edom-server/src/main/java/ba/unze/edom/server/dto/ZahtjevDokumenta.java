package ba.unze.edom.server.dto;

import ba.unze.edom.server.entity.VrstaDokumenta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jedan red na spisku potrebnih dokumenata.
 *
 * @param kljuc     stalni identifikator reda (za sidro u URL-u i napomene)
 * @param naziv     šta student treba priložiti
 * @param opis      napomena za studenta (može biti null)
 * @param obavezan  obavezni dokumenti idu svima; dodatni samo za označene kriterije
 * @param vrste     koje vrste dokumenata ispunjavaju ovaj zahtjev (student bira ako ih je više)
 * @param potrebno  koliko dokumenata treba (npr. po jedan za svakog člana domaćinstva)
 * @param poClanu   student uz dokument upisuje za kojeg člana je
 * @param prilozeni dokumenti koje je student već priložio za ovaj zahtjev
 */
public record ZahtjevDokumenta(
        String kljuc,
        String naziv,
        String opis,
        boolean obavezan,
        List<VrstaDokumenta> vrste,
        int potrebno,
        boolean poClanu,
        List<PrilozeniDokument> prilozeni
) {
    /**
     * Koliko je zahtjeva pokriveno. Kod dokumenata po članu broji se članove,
     * ne fajlove: tri platne liste za oca su jedan član.
     */
    public int prilozeno() {
        if (!poClanu) return prilozeni.size();
        return (int) prilozeni.stream()
                .map(d -> d.opis() == null ? "#" + d.idDokument() : d.opis().trim().toLowerCase())
                .distinct()
                .count();
    }

    /** Članovi za koje je već nešto priloženo (prijedlozi u polju "Za kojeg člana"). */
    public List<String> clanovi() {
        return prilozeni.stream()
                .map(PrilozeniDokument::opis)
                .filter(o -> o != null && !o.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toMap(
                        String::toLowerCase, o -> o, (prvi, drugi) -> prvi, java.util.LinkedHashMap::new))
                .values().stream()
                .toList();
    }

    public boolean ispunjen() {
        return prilozeno() >= potrebno;
    }

    /** Ako postoji samo jedna vrsta, forma ne prikazuje izbor. */
    public boolean jednaVrsta() {
        return vrste.size() == 1;
    }

    /** Vrsta nije podešena u bazi — upload nije moguć dok se ne doda šifra. */
    public boolean bezVrste() {
        return vrste.isEmpty();
    }

    // ---------------- napomena s linkovima ----------------

    /** Dio napomene: običan tekst (link == null) ili link. */
    public record Dio(String tekst, String link) {
        public boolean jeLink() { return link != null; }
    }

    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(([^)\\s]+)\\)");

    /**
     * Napomena razbijena na tekst i linkove. U napomeni se link piše
     * [tekst linka](/adresa), npr. "Podaci za uplatu su [ovdje](/upute#uplatnica)."
     */
    public List<Dio> opisDijelovi() {
        List<Dio> dijelovi = new ArrayList<>();
        if (opis == null) return dijelovi;
        Matcher m = LINK.matcher(opis);
        int kraj = 0;
        while (m.find()) {
            if (m.start() > kraj) dijelovi.add(new Dio(opis.substring(kraj, m.start()), null));
            dijelovi.add(new Dio(m.group(1), m.group(2)));
            kraj = m.end();
        }
        if (kraj < opis.length()) dijelovi.add(new Dio(opis.substring(kraj), null));
        return dijelovi;
    }
}