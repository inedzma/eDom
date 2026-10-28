package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.ObrazacPrijave;
import ba.unze.edom.server.dto.ObrazacPrijave.Pitanje;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.PrijavaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Prijavni obrazac popunjen iz online prijave (student ga štampa, potpisuje i šalje). */
@Service
@RequiredArgsConstructor
public class ObrazacPrijaveService {

    private final PrijavaRepository prijavaRepository;

    @Transactional(readOnly = true)
    public ObrazacPrijave zaStudenta(Integer idPrijave, Integer idStudenta) {
        Prijava p = prijavaRepository.zaDetalje(idPrijave)
                .filter(x -> x.getStudent() != null && Objects.equals(x.getStudent().getIdStudent(), idStudenta))
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));

        String status = p.getStatus() != null ? p.getStatus().getNaziv() : null;
        if (Statusi.U_IZRADI.equals(status)) {
            throw new PrijavaException("Prijavni obrazac možete odštampati nakon što podnesete prijavu.");
        }

        Student s = p.getStudent();
        Integer g = p.getAkademskaGodina();
        boolean brucos = UslovUpisa.jeBrucos(p);
        List<PrijavaKriterij> kr = p.getKriteriji() != null ? p.getKriteriji() : List.of();

        List<Pitanje> pitanja = new ArrayList<>();
        pitanja.add(new Pitanje(1, "Da li ste student koji upisuje \"godinu za godinom\" ili student koji prvi put upisuje studij?",
                brucos ? "Prvi put upisujem studij" : "Godinu za godinom"));
        pitanja.add(new Pitanje(2, "Da li je student brucoš/srednjoškolac?", brucos ? "Da" : "Ne"));
        pitanja.add(new Pitanje(3, "Koliko je prosjek Vaših ocjena?", broj(p.getProsjek())));
        pitanja.add(new Pitanje(4, "Koliko imate tačno položenih predmeta tokom studija?",
                brucos ? "—" : tekst(p.getPolozeniIspiti())));
        pitanja.add(new Pitanje(5, "Koju godinu studija upisujete?",
                p.getGodinaStudija() != null ? GodineStudija.naziv(p.getGodinaStudija()) : "—"));
        pitanja.add(new Pitanje(6, "Da li ste kao student osvojili nagrade u toku školovanja?",
                odgovor(kr, "nagrad")));
        pitanja.add(new Pitanje(7, "Da li ste kao student obnavljali istu godinu? Navesti akademske godine.",
                p.getUpisGodinePut() != null && p.getUpisGodinePut() >= 2
                        ? "Da – tekuću godinu upisujem " + p.getUpisGodinePut() + ". put" : "Ne"));
        pitanja.add(new Pitanje(8, "Da li ste student RVI? Ako jeste upišite stepen invalidnosti.",
                odgovor(kr, "STUDENT_RVI", "ratni vojni invalid")));
        pitanja.add(new Pitanje(9, "Da li ste dijete šehida ili poginulog borca?",
                odgovor(kr, "šehid", "sehid", "poginul")));
        pitanja.add(new Pitanje(10, "Da li vaši roditelji/staratelji imaju invalidnost? Ako imaju upišite stepen invalidnosti.",
                odgovor(kr, "INVALID_RODITELJA")));
        pitanja.add(new Pitanje(11, "Da li vaši roditelji imaju učešće u OS RBiH? Ako imaju upišite tačan broj mjeseci.",
                osRbih(kr)));
        pitanja.add(new Pitanje(12, "Da li ste dijete nosilaca ratnih priznanja? Ako jeste upišite broj ratnih priznanja.",
                odgovor(kr, "priznanj")));
        pitanja.add(new Pitanje(13, "Logoraški status: student logoraš upisati jedan (1), roditelj ili staratelj logoraš upisati dva (2)",
                odgovor(kr, "logora")));
        pitanja.add(new Pitanje(14, "Da li ste student bez roditelja: bez jednog roditelja (upisati 1) ili bez oba roditelja (upisati 2)?",
                odgovor(kr, "bez roditelj", "bez jednog", "bez oba")));
        pitanja.add(new Pitanje(15, "Da li ste student štićenik ustanove bez roditeljskog staranja?",
                odgovor(kr, "štićenik", "sticenik", "ustanov")));
        pitanja.add(new Pitanje(16, "Jesu li Vaši roditelji/staratelji korisnici socijalne pomoći?",
                odgovor(kr, "socijaln")));
        pitanja.add(new Pitanje(17, "Kolika su primanja po članu Vaše porodice?", primanjaPoClanu(p)));
        pitanja.add(new Pitanje(18, "Jeste li izbjeglo/raseljeno lice?",
                odgovor(kr, "izbjegl", "raseljen")));
        pitanja.add(new Pitanje(19, "Da li imate brata ili sestru koji studiraju?",
                odgovor(kr, "brat", "sestr")));
        pitanja.add(new Pitanje(20, "Kolika je Vaša tačna udaljenost od mjesta boravka od Studentskog centra?",
                p.getUdaljenostKm() != null ? broj(p.getUdaljenostKm()) + " km" : "—"));

        return new ObrazacPrijave(
                SifraStudenta.format(s.getSifraBroj(), g),
                g != null ? g + "/" + (g + 1) : "",
                spoji(" ", s.getIme(), s.getImeRoditelja() != null ? "(" + s.getImeRoditelja() + ")" : null, s.getPrezime()),
                spoji(", ", s.getFakultet(),
                        p.getGodinaStudija() != null ? GodineStudija.naziv(p.getGodinaStudija()) : null),
                s.getTelefon(),
                spoji(", ", s.getAdresa(), s.getOpcina(), s.getKanton()),
                spoji(", ", spoji(" ", s.getImeRoditelja(), s.getPrezime()),
                        s.getTelefonRoditelja() != null ? "tel. " + s.getTelefonRoditelja() : null),
                s.getEmail(),
                spoji(" / ", s.getJmbg(), s.getBrojLicneKarte() != null ? "LK " + s.getBrojLicneKarte() : null),
                pitanja);
    }

    // ---------- odgovori iz kriterija ----------

    /** "Da" (+ parametar) ako student ima kriterij čija šifra ili naziv sadrži neku od riječi; inače "Ne". */
    private String odgovor(List<PrijavaKriterij> kr, String... kljucevi) {
        for (PrijavaKriterij k : kr) {
            if (k.getVrsta() == null) continue;
            String sifra = nizak(k.getVrsta().getSifra());
            String naziv = nizak(k.getVrsta().getNaziv());
            for (String kljuc : kljucevi) {
                String kl = nizak(kljuc);
                if (sifra.contains(kl) || naziv.contains(kl)) {
                    String param = k.getParametar() != null
                            ? " – " + (k.getVrsta().getNazivParametra() != null ? k.getVrsta().getNazivParametra() + ": " : "")
                            + k.getParametar().stripTrailingZeros().toPlainString()
                            : "";
                    return "Da" + param;
                }
            }
        }
        return "Ne";
    }

    private String osRbih(List<PrijavaKriterij> kr) {
        for (PrijavaKriterij k : kr) {
            if (k.getVrsta() == null || !"DIJETE_OSRBIH".equals(k.getVrsta().getSifra())) continue;
            long mjeseci = k.getDatumOd() != null && k.getDatumDo() != null && !k.getDatumDo().isBefore(k.getDatumOd())
                    ? Period.between(k.getDatumOd(), k.getDatumDo()).toTotalMonths()
                    : (k.getParametar() != null ? k.getParametar().longValue() : 0);
            return "Da – " + mjeseci + " mjeseci";
        }
        return "Ne";
    }

    private String primanjaPoClanu(Prijava p) {
        if (p.getUkupnaPrimanja() == null || p.getBrojClanovaDomacinstva() == null || p.getBrojClanovaDomacinstva() < 1) {
            return "—";
        }
        BigDecimal poClanu = p.getUkupnaPrimanja()
                .divide(BigDecimal.valueOf(p.getBrojClanovaDomacinstva()), 2, RoundingMode.HALF_UP);
        return poClanu.toPlainString() + " KM (" + p.getBrojClanovaDomacinstva() + " članova domaćinstva)";
    }

    // ---------- pomoćne ----------

    private static String nizak(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static String broj(BigDecimal b) {
        return b == null ? "—" : b.stripTrailingZeros().toPlainString();
    }

    private static String tekst(Object o) {
        return o == null ? "—" : o.toString();
    }

    private static String spoji(String sep, String... dijelovi) {
        List<String> l = new ArrayList<>();
        for (String d : dijelovi) if (d != null && !d.isBlank()) l.add(d.trim());
        return String.join(sep, l);
    }
}