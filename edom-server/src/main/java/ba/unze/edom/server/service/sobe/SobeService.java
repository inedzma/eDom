package ba.unze.edom.server.service.sobe;

import ba.unze.edom.common.dto.sobe.*;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.*;
import ba.unze.edom.server.service.prijava.Statusi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sobe i smještaj (samo desktop, interna evidencija).
 * Pregled soba, raspored po pravilima, ručna dodjela, useljenje i iseljenje.
 */
@Service
@RequiredArgsConstructor
public class SobeService {

    /** Prijave koje ulaze u raspored. */
    static final List<String> STATUSI_ZA_RASPORED = List.of(Statusi.ODOBRENO, Statusi.ZAKLJUCENO);

    private final SobaRepository sobaRepository;
    private final SmjestajRepository smjestajRepository;
    private final ZeljaSmjestajaRepository zeljaRepository;
    private final OznakaStanaraRepository oznakaRepository;
    private final PrijavaRepository prijavaRepository;
    private final StatusPrijaveRepository statusRepository;

    // =====================================================================
    //  PREGLED
    // =====================================================================

    @Transactional(readOnly = true)
    public SobePregledDTO pregled(int godina) {
        List<Soba> sobe = sobaRepository.sveSaSpratom();
        List<Smjestaj> smjestaji = smjestajRepository.aktivniZaGodinu(godina);
        List<Prijava> zaRaspored = smjestajRepository.zaRaspored(godina, STATUSI_ZA_RASPORED);
        Map<Integer, Integer> rang = rangovi(zaRaspored);

        Map<Integer, List<Smjestaj>> poSobi = smjestaji.stream()
                .collect(Collectors.groupingBy(sm -> sm.getSoba().getIdSoba()));

        List<SobaPregledDTO> lista = new ArrayList<>();
        int ukupno = 0, zauzeto = 0, slobodnoM = 0, slobodnoZ = 0;

        for (Soba s : sobe) {
            List<Smjestaj> stanari = poSobi.getOrDefault(s.getIdSoba(), List.of());
            String spol = spolSobe(s, stanari);

            List<StanarDTO> st = stanari.stream()
                    .sorted(Comparator.comparing(sm -> imePrezime(sm.getPrijava())))
                    .map(sm -> stanar(sm, rang))
                    .toList();

            lista.add(new SobaPregledDTO(
                    s.getIdSoba(), s.getBroj(), s.getSprat().getNaziv(), s.getSprat().getRedoslijed(),
                    s.getKapacitet(), spol,
                    s.getKategorija().getNaziv(), s.getKategorija().getBoja(), s.getKategorija().isZaStudente(),
                    s.isAktivna(), s.getBrojKljuceva(), s.getNapomena(), st));

            if (s.zaRaspored()) {
                ukupno += s.getKapacitet();
                zauzeto += Math.min(stanari.size(), s.getKapacitet());
                int slobodno = Math.max(0, s.getKapacitet() - stanari.size());
                if ("M".equals(spol)) slobodnoM += slobodno;
                if ("Z".equals(spol)) slobodnoZ += slobodno;
            }
        }

        Set<Integer> imajuSobu = smjestaji.stream()
                .map(sm -> sm.getPrijava().getIdPrijava()).collect(Collectors.toSet());
        int bezSobe = (int) zaRaspored.stream().filter(p -> !imajuSobu.contains(p.getIdPrijava())).count();

        return new SobePregledDTO(godina, lista, ukupno, zauzeto, slobodnoM, slobodnoZ, bezSobe);
    }

    /** Odobrene prijave bez sobe, po rang listi. */
    @Transactional(readOnly = true)
    public List<StudentBezSobeDTO> bezSobe(int godina) {
        List<Prijava> zaRaspored = smjestajRepository.zaRaspored(godina, STATUSI_ZA_RASPORED);
        Map<Integer, Integer> rang = rangovi(zaRaspored);
        Set<Integer> imajuSobu = smjestajRepository.aktivniZaGodinu(godina).stream()
                .map(sm -> sm.getPrijava().getIdPrijava()).collect(Collectors.toSet());
        Map<Integer, ZeljaSmjestaja> zelje = zeljaRepository.zaGodinu(godina).stream()
                .collect(Collectors.toMap(z -> z.getPrijava().getIdPrijava(), z -> z, (a, b) -> a));

        return zaRaspored.stream()
                .filter(p -> !imajuSobu.contains(p.getIdPrijava()))
                .map(p -> new StudentBezSobeDTO(
                        p.getIdPrijava(), imePrezime(p), spolStudenta(p), p.getGodinaStudija(),
                        rang.get(p.getIdPrijava()), broj(p.getUkupniBodovi()),
                        opisZelje(zelje.get(p.getIdPrijava())),
                        p.getStudent().getBrojIndeksa(), p.getStudent().getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OznakaStanaraDTO> oznake() {
        return oznakaRepository.findAll().stream()
                .map(o -> new OznakaStanaraDTO(o.getIdOznaka(), o.getNaziv(), o.getBoja()))
                .toList();
    }

    // =====================================================================
    //  PRIJEDLOG RASPOREDA
    // =====================================================================

    /**
     * Briše stari nepotvrđeni prijedlog i pravi novi za sve odobrene bez sobe.
     * Potvrđeni i useljeni ostaju gdje jesu.
     */
    @Transactional
    public PrijedlogRasporedaDTO napraviPrijedlog(int godina) {
        smjestajRepository.obrisiPrijedlog(godina);
        smjestajRepository.flush();

        List<Prijava> zaRaspored = smjestajRepository.zaRaspored(godina, STATUSI_ZA_RASPORED);
        Map<Integer, Integer> rang = rangovi(zaRaspored);
        List<Smjestaj> postojeci = smjestajRepository.aktivniZaGodinu(godina);
        Set<Integer> imajuSobu = postojeci.stream()
                .map(sm -> sm.getPrijava().getIdPrijava()).collect(Collectors.toSet());

        // ---- sobe sa slobodnim mjestima ----
        Map<Integer, List<Smjestaj>> poSobi = postojeci.stream()
                .collect(Collectors.groupingBy(sm -> sm.getSoba().getIdSoba()));
        Map<Integer, Soba> sobePoId = new HashMap<>();
        List<RasporedAlgoritam.SobaStanje> stanje = new ArrayList<>();
        for (Soba s : sobaRepository.sveSaSpratom()) {
            if (!s.zaRaspored()) continue;
            List<Smjestaj> u = poSobi.getOrDefault(s.getIdSoba(), List.of());
            int slobodno = s.getKapacitet() - u.size();
            if (slobodno <= 0) continue;
            sobePoId.put(s.getIdSoba(), s);
            stanje.add(new RasporedAlgoritam.SobaStanje(
                    s.getIdSoba(), s.getBroj(), s.getKapacitet(), spolSobe(s, u), slobodno));
        }

        // ---- kandidati: odobreni bez sobe, s željama ----
        List<Prijava> cekaju = zaRaspored.stream().filter(p -> !imajuSobu.contains(p.getIdPrijava())).toList();
        Map<Integer, ZeljaSmjestaja> zelje = zeljaRepository.zaGodinu(godina).stream()
                .collect(Collectors.toMap(z -> z.getPrijava().getIdPrijava(), z -> z, (a, b) -> a));
        Map<String, Integer> poOznaci = new HashMap<>();   // broj indeksa / email -> prijava
        for (Prijava p : cekaju) {
            Student st = p.getStudent();
            if (st.getBrojIndeksa() != null) poOznaci.put(st.getBrojIndeksa().trim().toLowerCase(), p.getIdPrijava());
            if (st.getEmail() != null) poOznaci.put(st.getEmail().trim().toLowerCase(), p.getIdPrijava());
        }

        List<RasporedAlgoritam.Kandidat> kandidati = new ArrayList<>();
        for (Prijava p : cekaju) {
            ZeljaSmjestaja z = zelje.get(p.getIdPrijava());
            Set<Integer> cimeri = new HashSet<>();
            if (z != null) {
                for (String c : new String[]{z.getCimer1(), z.getCimer2()}) {
                    if (c == null || c.isBlank()) continue;
                    Integer id = poOznaci.get(c.trim().toLowerCase());
                    if (id != null && !id.equals(p.getIdPrijava())) cimeri.add(id);
                }
            }
            kandidati.add(new RasporedAlgoritam.Kandidat(
                    p.getIdPrijava(), spolStudenta(p),
                    p.getGodinaStudija() != null ? p.getGodinaStudija() : 1,
                    rang.get(p.getIdPrijava()),
                    z != null ? z.getZeljeniKapacitet() : null,
                    z != null && z.getSoba() != null ? z.getSoba().getIdSoba() : null,
                    cimeri));
        }

        // ---- raspored i upis prijedloga ----
        List<RasporedAlgoritam.Dodjela> dodjele = RasporedAlgoritam.rasporedi(kandidati, stanje);
        Map<Integer, Prijava> prijavePoId = cekaju.stream()
                .collect(Collectors.toMap(Prijava::getIdPrijava, p -> p));

        List<PrijedlogRasporedaDTO.Stavka> stavke = new ArrayList<>();
        int rasporedjeno = 0;
        for (RasporedAlgoritam.Dodjela d : dodjele) {
            Prijava p = prijavePoId.get(d.idPrijava());
            if (d.idSoba() != null) {
                Smjestaj sm = new Smjestaj();
                sm.setPrijava(p);
                sm.setSoba(sobePoId.get(d.idSoba()));
                sm.setStatus(Smjestaj.PREDLOZEN);
                sm.setRazlog(skrati(d.razlog()));
                smjestajRepository.save(sm);
                rasporedjeno++;
            }
            stavke.add(new PrijedlogRasporedaDTO.Stavka(
                    p.getIdPrijava(), rang.get(p.getIdPrijava()), imePrezime(p), spolStudenta(p),
                    p.getGodinaStudija(), d.brojSobe(), d.kapacitetSobe(), d.razlog(), d.zeljaIspunjena()));
        }

        return new PrijedlogRasporedaDTO(stavke, rasporedjeno, stavke.size() - rasporedjeno);
    }

    /** Prijedlog postaje dodjela (student još nije uselio). */
    @Transactional
    public int potvrdiPrijedlog(int godina) {
        return smjestajRepository.potvrdiPrijedlog(godina);
    }

    @Transactional
    public int ponistiPrijedlog(int godina) {
        return smjestajRepository.obrisiPrijedlog(godina);
    }

    // =====================================================================
    //  RUČNE IZMJENE
    // =====================================================================

    /** Dodjela u sobu ili premještaj, ako student već ima sobu. */
    @Transactional
    public void dodijeli(DodjelaSobeDTO z) {
        Prijava p = prijavaRepository.findById(z.idPrijava())
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));
        String status = p.getStatus() != null ? p.getStatus().getNaziv() : "";
        if (!STATUSI_ZA_RASPORED.contains(status)) {
            throw new PrijavaException("Sobu može dobiti samo odobrena prijava (ova je: " + status + ").");
        }

        Soba s = sobaRepository.findById(z.idSoba())
                .orElseThrow(() -> new PrijavaException("Soba ne postoji."));
        if (!s.isAktivna()) throw new PrijavaException("Soba " + s.getBroj() + " je van upotrebe.");
        if (!s.getKategorija().isZaStudente()) {
            throw new PrijavaException("Soba " + s.getBroj() + " nije za studente (" + s.getKategorija().getNaziv() + ").");
        }

        int godina = p.getAkademskaGodina();
        Optional<Smjestaj> postojeci = smjestajRepository.aktivniZaPrijavu(p.getIdPrijava());
        if (postojeci.isPresent() && postojeci.get().getSoba().getIdSoba().equals(s.getIdSoba())) return;

        long zauzeto = smjestajRepository.zauzetoUSobi(s.getIdSoba(), godina);
        if (zauzeto >= s.getKapacitet()) {
            throw new PrijavaException("Soba " + s.getBroj() + " je puna (" + zauzeto + "/" + s.getKapacitet() + ").");
        }

        String spolStudenta = spolStudenta(p);
        List<Smjestaj> uSobi = smjestajRepository.aktivniZaGodinu(godina).stream()
                .filter(sm -> sm.getSoba().getIdSoba().equals(s.getIdSoba())).toList();
        String spolSobe = spolSobe(s, uSobi);
        if (spolSobe != null && spolStudenta != null && !spolSobe.equals(spolStudenta)) {
            throw new PrijavaException("Soba " + s.getBroj() + " je " + ("M".equals(spolSobe) ? "muška" : "ženska")
                    + ". Ako treba, promijenite spol sprata ili sobe.");
        }

        Smjestaj sm = postojeci.orElseGet(Smjestaj::new);
        boolean premjestaj = postojeci.isPresent();
        sm.setPrijava(p);
        sm.setSoba(s);
        if (!premjestaj || Smjestaj.PREDLOZEN.equals(sm.getStatus())) sm.setStatus(Smjestaj.DODIJELJEN);
        sm.setRazlog(premjestaj ? "Premješten ručno (administrator)" : "Dodijeljeno ručno (administrator)");
        smjestajRepository.save(sm);
    }

    /** Student je uselio i preuzeo ključeve; prijava postaje "zaključeno". */
    @Transactional
    public void useli(Integer idSmjestaja, int kljucevi) {
        Smjestaj sm = smjestaj(idSmjestaja);
        if (Smjestaj.USELJEN.equals(sm.getStatus())) return;
        sm.setStatus(Smjestaj.USELJEN);
        sm.setDatumUseljenja(LocalDate.now());
        sm.setKljuceviPreuzeti(Math.max(0, kljucevi));

        Prijava p = sm.getPrijava();
        if (p.getStatus() != null && Statusi.ODOBRENO.equals(p.getStatus().getNaziv())) {
            statusRepository.findByNaziv(Statusi.ZAKLJUCENO).ifPresent(p::setStatus);
        }
    }

    /** Iseljenje; vraća koliko ključeva još fali (0 = sve vraćeno). */
    /** Iseljenje: student više nije u domu → prijava "iseljen". Vraća koliko ključeva još fali. */
    @Transactional
    public int iseli(Integer idSmjestaja, int vraceno) {
        Smjestaj sm = smjestaj(idSmjestaja);
        sm.setStatus(Smjestaj.ISELJEN);
        sm.setDatumIseljenja(LocalDate.now());
        sm.setKljuceviVraceni(Math.max(0, vraceno));

        // bez ovoga bi se iseljeni ponovo pojavio među "odobrenima bez sobe" i dobio sobu u prijedlogu
        Prijava p = sm.getPrijava();
        String status = p.getStatus() != null ? p.getStatus().getNaziv() : null;
        if (Statusi.ODOBRENO.equals(status) || Statusi.ZAKLJUCENO.equals(status)) {
            statusRepository.findByNaziv(Statusi.ISELJEN).ifPresent(p::setStatus);
        }
        return Math.max(0, sm.getKljuceviPreuzeti() - sm.getKljuceviVraceni());
    }

    /** Uklanjanje dodjele (samo dok student nije uselio). */
    @Transactional
    public void ukloni(Integer idSmjestaja) {
        Smjestaj sm = smjestaj(idSmjestaja);
        if (Smjestaj.USELJEN.equals(sm.getStatus())) {
            throw new PrijavaException("Student je već uselio. Koristite iseljenje.");
        }
        smjestajRepository.delete(sm);
    }

    @Transactional
    public void postaviOznaku(Integer idSmjestaja, Integer idOznake) {
        Smjestaj sm = smjestaj(idSmjestaja);
        sm.setOznaka(idOznake == null ? null : oznakaRepository.findById(idOznake)
                .orElseThrow(() -> new PrijavaException("Oznaka ne postoji.")));
    }

    /** Soba u upotrebi ili ne (kvar, renoviranje). */
    @Transactional
    public void postaviAktivna(Integer idSobe, boolean aktivna, String napomena) {
        Soba s = sobaRepository.findById(idSobe).orElseThrow(() -> new PrijavaException("Soba ne postoji."));
        s.setAktivna(aktivna);
        if (napomena != null) s.setNapomena(napomena.isBlank() ? null : napomena.trim());
    }

    // =====================================================================
    //  POMOĆNE
    // =====================================================================

    private Smjestaj smjestaj(Integer id) {
        return smjestajRepository.findById(id).orElseThrow(() -> new PrijavaException("Smještaj ne postoji."));
    }

    /** Pozicija na rang listi: redoslijed iz upita (bodovi opadajuće). */
    private static Map<Integer, Integer> rangovi(List<Prijava> poRedu) {
        Map<Integer, Integer> r = new HashMap<>();
        for (int i = 0; i < poRedu.size(); i++) r.put(poRedu.get(i).getIdPrijava(), i + 1);
        return r;
    }

    /** Spol sobe: soba, pa sprat, pa trenutni stanari. */
    private String spolSobe(Soba s, List<Smjestaj> stanari) {
        String spol = normalizuj(s.spolSobe());
        if (spol != null) return spol;
        return stanari.stream().map(sm -> spolStudenta(sm.getPrijava())).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static String spolStudenta(Prijava p) {
        return p.getStudent() != null ? normalizuj(p.getStudent().getSpol()) : null;
    }

    /** 'M' ili 'Z' iz bilo kojeg zapisa ("M", "Ž", "muško", "ZENSKO"...). */
    static String normalizuj(Object spol) {
        if (spol == null) return null;
        String s = spol.toString().trim().toUpperCase();
        if (s.isEmpty()) return null;
        char c = s.charAt(0);
        if (c == 'M') return "M";
        if (c == 'Z' || c == 'Ž') return "Z";
        return null;
    }

    private static String imePrezime(Prijava p) {
        Student s = p.getStudent();
        return s == null ? "" : s.getPrezime() + " " + s.getIme();
    }

    private StanarDTO stanar(Smjestaj sm, Map<Integer, Integer> rang) {
        Prijava p = sm.getPrijava();
        OznakaStanara o = sm.getOznaka();
        return new StanarDTO(sm.getIdSmjestaj(), p.getIdPrijava(), imePrezime(p), spolStudenta(p),
                p.getGodinaStudija(), rang.get(p.getIdPrijava()), sm.getStatus(),
                o != null ? o.getIdOznaka() : null, o != null ? o.getNaziv() : null, o != null ? o.getBoja() : null,
                sm.getKljuceviPreuzeti(), sm.getRazlog());
    }

    private static String opisZelje(ZeljaSmjestaja z) {
        if (z == null) return null;
        List<String> d = new ArrayList<>();
        if (z.getZeljeniKapacitet() != null) d.add(z.getZeljeniKapacitet() == 2 ? "dvokrevetna" : "trokrevetna");
        if (z.getSoba() != null) d.add("soba " + z.getSoba().getBroj());
        if (z.getCimer1() != null && !z.getCimer1().isBlank()) d.add("cimer " + z.getCimer1());
        if (z.getCimer2() != null && !z.getCimer2().isBlank()) d.add("cimer " + z.getCimer2());
        return d.isEmpty() ? null : String.join(", ", d);
    }

    private static double broj(Number n) {
        return n == null ? 0 : n.doubleValue();
    }

    private static String skrati(String s) {
        return s == null || s.length() <= 255 ? s : s.substring(0, 255);
    }
}