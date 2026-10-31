package ba.unze.edom.server.service.bodovanje;

import ba.unze.edom.common.dto.bodovanje.KategorijaBodovanja;
import ba.unze.edom.common.dto.bodovanje.RezultatBodovanja;
import ba.unze.edom.common.dto.bodovanje.StavkaBodovanja;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.entity.VrstaKriterija;
import ba.unze.edom.server.service.opste.PostavkeService;
import ba.unze.edom.server.service.prijava.GodineStudija;
import ba.unze.edom.server.service.prijava.PrijavaValidator;
import ba.unze.edom.server.service.prijava.Statusi;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Racuna bodove prijave prema Pravilniku Studentskog centra.
 * Brojevi za uspjeh, udaljenost, primanja i ponovni upis dolaze iz Postavki (PravilaBodovanja);
 * bodovi kriterija iz tabele vrsta_kriterija.
 *
 *  - Branioci/socijalni:    boduje se SAMO najvisa
 *  - Dodatni kriteriji:     sabiraju se
 */
@Service
public class BodovanjeService {

    /** Podrazumijevana kazna; stvarna vrijednost je u Postavkama. */
    static final double KAZNA_PONOVNI_UPIS = -3;

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    private final PrijavaValidator validator;
    private final PostavkeService postavke;

    public BodovanjeService(PrijavaValidator validator, PostavkeService postavke) {
        this.validator = validator;
        this.postavke = postavke;
    }

    public RezultatBodovanja izracunaj(Prijava p) {

        if (p == null) {
            return new RezultatBodovanja(0, 0, false, List.of());
        }

        validator.provjeri(p);
        PravilaBodovanja pr = postavke.pravila();

        StavkaBodovanja uspjeh     = bodoviUspjeh(p, pr);
        StavkaBodovanja udaljenost = bodoviUdaljenost(p, pr);
        StavkaBodovanja primanja   = bodoviPrimanja(p, pr);
        Optional<StavkaBodovanja> ponovniUpis = kaznaPonovniUpis(p, pr);

        var braniociSvi        = najveciBranilac(p, false);
        var braniociPotvrdjeni = najveciBranilac(p, true);

        List<StavkaBodovanja> dodatni = dodatniKriteriji(p);

        List<StavkaBodovanja> stavke = new ArrayList<>();
        stavke.add(uspjeh);
        ponovniUpis.ifPresent(stavke::add);
        stavke.add(udaljenost);
        stavke.add(primanja);
        braniociSvi.ifPresent(stavke::add);
        stavke.addAll(dodatni);

        List<StavkaBodovanja> osnovni = new ArrayList<>(List.of(uspjeh, udaljenost, primanja));
        ponovniUpis.ifPresent(osnovni::add);

        double potvrdjeno =
                zbir(osnovni, true)
                        + braniociPotvrdjeni.map(StavkaBodovanja::bodovi).orElse(0.0)
                        + zbir(dodatni, true);

        double potencijalno =
                zbir(osnovni, false)
                        + braniociSvi.map(StavkaBodovanja::bodovi).orElse(0.0)
                        + zbir(dodatni, false);

        String st = nazivStatusa(p);
        boolean konacno = Statusi.ODOBRENO.equals(st) || Statusi.ZAKLJUCENO.equals(st);

        return new RezultatBodovanja(
                zaokruzi(potvrdjeno), zaokruzi(potencijalno), konacno, stavke, upozorenja(p));
    }

    private double zbir(List<StavkaBodovanja> stavke, boolean samoVerifikovane) {
        return stavke.stream()
                .filter(s -> !samoVerifikovane || s.verifikovan())
                .mapToDouble(StavkaBodovanja::bodovi)
                .sum();
    }

    private String nazivStatusa(Prijava p) {
        return p.getStatus() != null ? p.getStatus().getNaziv() : null;
    }

    // ---------------- UPOZORENJA ----------------

    private List<String> upozorenja(Prijava p) {
        List<String> u = new ArrayList<>();
        Integer put = p.getUpisGodinePut();
        if (put != null && put >= 3) {
            u.add("Student upisuje istu godinu studija " + put + ". put. "
                    + "Prema Pravilniku nema pravo na smještaj; prijava se razmatra samo ako ostanu slobodna mjesta.");
        }
        return u;
    }

    // ---------------- USPJEH ----------------

    private StavkaBodovanja bodoviUspjeh(Prijava p, PravilaBodovanja pr) {

        Integer godina = p.getGodinaStudija();
        BigDecimal prosjek = p.getProsjek();
        boolean ver = p.isUspjehVerifikovan();

        if (godina == null || prosjek == null) {
            return new StavkaBodovanja("Uspjeh", 0,
                    "Prosjek ili godina studija nisu uneseni", ver,
                    KategorijaBodovanja.USPJEH);
        }

        double prs = prosjek.doubleValue();

        if (godina == GodineStudija.PRVA) {
            double b = prs * pr.brucosMnozilac() + pr.brucosDodatak();
            return new StavkaBodovanja("Uspjeh (brucoš)", zaokruzi(b),
                    "Prosjek " + prs, ver,
                    KategorijaBodovanja.USPJEH);
        }

        int ispiti = p.getPolozeniIspiti() != null ? p.getPolozeniIspiti() : 0;
        double b = prs * pr.prosjekMnozilac() + ispiti * pr.ispitMnozilac() + pr.bonus(godina);

        return new StavkaBodovanja("Uspjeh", zaokruzi(b),
                "Prosjek " + prs + ", položenih ispita: " + ispiti
                        + ", " + GodineStudija.naziv(godina), ver,
                KategorijaBodovanja.USPJEH);
    }

    double bonusGodine(int godina) {
        return postavke.pravila().bonus(godina);
    }

    private Optional<StavkaBodovanja> kaznaPonovniUpis(Prijava p, PravilaBodovanja pr) {
        Integer put = p.getUpisGodinePut();
        if (put == null || put < 2 || pr.kaznaPonovniUpis() == 0) return Optional.empty();

        return Optional.of(new StavkaBodovanja("Ponovni upis godine", pr.kaznaPonovniUpis(),
                "Tekuću godinu upisuje " + put + ". put", p.isUspjehVerifikovan(),
                KategorijaBodovanja.USPJEH));
    }

    // ---------------- UDALJENOST ----------------

    private StavkaBodovanja bodoviUdaljenost(Prijava p, PravilaBodovanja pr) {

        boolean ver = p.isUdaljenostVerifikovana();

        if (p.getUdaljenostKm() == null) {
            return new StavkaBodovanja("Udaljenost", 0, "Nije uneseno", ver,
                    KategorijaBodovanja.UDALJENOST);
        }

        double km = p.getUdaljenostKm().doubleValue();
        return new StavkaBodovanja("Udaljenost", pr.udaljenost().bodovi(km),
                String.format("%.1f km", km), ver,
                KategorijaBodovanja.UDALJENOST);
    }

    // ---------------- PRIMANJA ----------------

    private StavkaBodovanja bodoviPrimanja(Prijava p, PravilaBodovanja pr) {

        boolean ver = p.isPrimanjaVerifikovana();

        Integer clanova = p.getBrojClanovaDomacinstva();
        BigDecimal ukupno = p.getUkupnaPrimanja();

        if (ukupno == null || clanova == null || clanova < 1) {
            return new StavkaBodovanja("Primanja domaćinstva", 0,
                    "Nije uneseno", ver,
                    KategorijaBodovanja.PRIMANJA);
        }

        double poClanu = ukupno.doubleValue() / clanova;

        return new StavkaBodovanja("Primanja domaćinstva", pr.primanja().bodovi(poClanu),
                String.format("%.2f KM po članu (%d članova)", poClanu, clanova),
                ver,
                KategorijaBodovanja.PRIMANJA);
    }

    // ---------------- BRANIOCI ----------------

    private Optional<StavkaBodovanja> najveciBranilac(Prijava p, boolean samoVerifikovani) {
        if (p.getKriteriji() == null) return Optional.empty();

        return p.getKriteriji().stream()
                .filter(k -> k.getVrsta() != null)
                .filter(k -> GrupeKriterija.BRANIOCI.equals(k.getVrsta().getGrupa()))
                .filter(k -> !samoVerifikovani || k.isVerifikovan())
                .map(this::stavkaZaKriterij)
                .max(Comparator.comparingDouble(StavkaBodovanja::bodovi));
    }

    // ---------------- DODATNI ----------------

    private List<StavkaBodovanja> dodatniKriteriji(Prijava p) {
        if (p.getKriteriji() == null) return List.of();

        Map<String, PrijavaKriterij> jedinstveni = new LinkedHashMap<>();
        for (PrijavaKriterij k : p.getKriteriji()) {
            if (k.getVrsta() == null) continue;
            if (!GrupeKriterija.DODATNI.equals(k.getVrsta().getGrupa())) continue;
            jedinstveni.putIfAbsent(k.getVrsta().getSifra(), k);
        }

        return jedinstveni.values().stream()
                .map(this::stavkaZaKriterij)
                .toList();
    }

    // ---------------- ZAJEDNICKO ----------------

    /** Stavka za jedan kriterij. Koristi je i admin pregled (BodovanjeAdminService). */
    public StavkaBodovanja stavkaZaKriterij(PrijavaKriterij k) {

        VrstaKriterija v = k.getVrsta();

        if (v.getBodovi() != null) {
            String obr = v.getNazivParametra() != null && k.getParametar() != null
                    ? v.getNazivParametra() + ": " + k.getParametar().stripTrailingZeros().toPlainString()
                    : null;
            return new StavkaBodovanja(v.getNaziv(),
                    v.getBodovi().doubleValue(), obr, k.isVerifikovan(),
                    v.getGrupa());
        }

        if ("DIJETE_OSRBIH".equals(v.getSifra())) {
            return stavkaOsRbih(k);
        }

        double param = k.getParametar() != null ? k.getParametar().doubleValue() : 0;

        double b = switch (v.getSifra()) {
            case "STUDENT_RVI"       -> param == 0 ? 0 : (param < 60 ? 15 : 20);
            case "INVALID_RODITELJA" -> param * 0.1;
            default -> 0;
        };

        String obrazlozenje = v.getNazivParametra() != null
                ? v.getNazivParametra() + ": " + param
                : null;

        return new StavkaBodovanja(v.getNaziv(), zaokruzi(b),
                obrazlozenje, k.isVerifikovan(),
                v.getGrupa());
    }

    private StavkaBodovanja stavkaOsRbih(PrijavaKriterij k) {
        VrstaKriterija v = k.getVrsta();
        long mjeseci;
        String obr;

        if (k.getDatumOd() != null && k.getDatumDo() != null) {
            mjeseci = punihMjeseci(k.getDatumOd(), k.getDatumDo());
            obr = k.getDatumOd().format(DATUM) + " – " + k.getDatumDo().format(DATUM)
                    + " (" + mjeseci + " punih mjeseci)";
        } else {
            mjeseci = k.getParametar() != null ? k.getParametar().longValue() : 0;
            obr = mjeseci + " mjeseci";
        }

        return new StavkaBodovanja(v.getNaziv(), zaokruzi(mjeseci * 0.3),
                obr, k.isVerifikovan(), v.getGrupa());
    }

    static long punihMjeseci(LocalDate od, LocalDate doDatuma) {
        if (od == null || doDatuma == null || doDatuma.isBefore(od)) return 0;
        return Period.between(od, doDatuma).toTotalMonths();
    }

    private double zaokruzi(double d) {
        return Math.round(d * 100.0) / 100.0;
    }
}