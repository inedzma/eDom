package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.KategorijaBodovanja;
import ba.unze.edom.common.dto.RezultatBodovanja;
import ba.unze.edom.common.dto.StavkaBodovanja;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.entity.VrstaKriterija;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Racuna bodove prijave prema Pravilniku Studentskog centra (2026/2027).
 *
 * Bodovi se NIKAD ne akumuliraju u bazi - uvijek se racunaju iznova.
 * Zbog toga je svaka ispravka podataka odmah vidljiva u rezultatu,
 * a rezultat je uvijek objasnjiv kroz listu stavki.
 *
 * Pregled pravila:
 *  - Uspjeh, brucos:        prosjek x 9 + 1
 *  - Uspjeh, ostali:        prosjek x 3,5 + polozeni ispiti x 1,8 + bonus za godinu
 *                           (2.=3, 3.=5, 4.=7, 5.=9, 6.=10, apsolvent=0, postdiplomac=10)
 *  - Ponovni upis godine:   -3 (drugi put i vise); treci put i vise = upozorenje za admina
 *  - Udaljenost:            <50 km 0, <80 4, <120 8, <160 12, ostalo 15
 *  - Primanja po clanu:     <110 KM 20, <=150 15, <=200 8, <=300 4, <=400 2, ostalo 0
 *  - Boracke/socijalne:     boduje se SAMO najvisa
 *  - Dodatni kriteriji:     sabiraju se (izbjeglica 3, brat/sestra 2, nagrade 3)
 */
@Service
public class BodovanjeService {

    /** Negativni bodovi za ponovni upis iste godine studija. */
    static final double KAZNA_PONOVNI_UPIS = -3;

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    private final PrijavaValidator validator;

    public BodovanjeService(PrijavaValidator validator) {
        this.validator = validator;
    }

    public RezultatBodovanja izracunaj(Prijava p) {

        if (p == null) {
            return new RezultatBodovanja(0, 0, false, List.of());
        }

        validator.provjeri(p);

        // --- osnovni kriteriji, svaki sa svojim flagom verifikacije ---
        StavkaBodovanja uspjeh     = bodoviUspjeh(p);
        StavkaBodovanja udaljenost = bodoviUdaljenost(p);
        StavkaBodovanja primanja   = bodoviPrimanja(p);
        Optional<StavkaBodovanja> ponovniUpis = kaznaPonovniUpis(p);

        // --- branioci: najveci, odvojeno za potvrdjeno i potencijalno ---
        var braniociSvi        = najveciBranilac(p, false);
        var braniociPotvrdjeni = najveciBranilac(p, true);

        // --- dodatni: zbrajaju se ---
        List<StavkaBodovanja> dodatni = dodatniKriteriji(p);

        // --- prikaz ---
        List<StavkaBodovanja> stavke = new ArrayList<>();
        stavke.add(uspjeh);
        ponovniUpis.ifPresent(stavke::add);
        stavke.add(udaljenost);
        stavke.add(primanja);
        braniociSvi.ifPresent(stavke::add);
        stavke.addAll(dodatni);

        // --- zbirovi ---
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

    /** Stvari koje ne blokiraju prijavu, ali ih administrator mora vidjeti. */
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

    private StavkaBodovanja bodoviUspjeh(Prijava p) {

        Integer godina = p.getGodinaStudija();
        BigDecimal prosjek = p.getProsjek();
        boolean ver = p.isUspjehVerifikovan();

        if (godina == null || prosjek == null) {
            return new StavkaBodovanja("Uspjeh", 0,
                    "Prosjek ili godina studija nisu uneseni", ver,
                    KategorijaBodovanja.USPJEH);
        }

        double pr = prosjek.doubleValue();

        if (godina == GodineStudija.PRVA) {
            double b = pr * 9 + 1;
            return new StavkaBodovanja("Uspjeh (brucoš)", zaokruzi(b),
                    "Prosjek " + pr, ver,
                    KategorijaBodovanja.USPJEH);
        }

        int ispiti = p.getPolozeniIspiti() != null ? p.getPolozeniIspiti() : 0;
        double b = pr * 3.5 + ispiti * 1.8 + bonusGodine(godina);

        return new StavkaBodovanja("Uspjeh", zaokruzi(b),
                "Prosjek " + pr + ", položenih ispita: " + ispiti
                        + ", " + GodineStudija.naziv(godina), ver,
                KategorijaBodovanja.USPJEH);
    }

    /** Bonus za godinu studija. Apsolvent nema bonus. */
    double bonusGodine(int godina) {
        return switch (godina) {
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 7;
            case 5 -> 9;
            case 6 -> 10;
            case GodineStudija.APSOLVENT -> 0;
            case GodineStudija.POSTDIPLOMAC -> 10;
            default -> 0;
        };
    }

    /**
     * Pitanje 1 obrasca: ako student istu godinu upisuje drugi put ili vise, -3 boda.
     * Verifikuje se zajedno s uspjehom, jer se dokazuje istim uvjerenjem s fakulteta.
     */
    private Optional<StavkaBodovanja> kaznaPonovniUpis(Prijava p) {
        Integer put = p.getUpisGodinePut();
        if (put == null || put < 2) return Optional.empty();

        return Optional.of(new StavkaBodovanja("Ponovni upis godine", KAZNA_PONOVNI_UPIS,
                "Tekuću godinu upisuje " + put + ". put", p.isUspjehVerifikovan(),
                KategorijaBodovanja.USPJEH));
    }

    // ---------------- UDALJENOST ----------------

    private StavkaBodovanja bodoviUdaljenost(Prijava p) {

        boolean ver = p.isUdaljenostVerifikovana();

        if (p.getUdaljenostKm() == null) {
            return new StavkaBodovanja("Udaljenost", 0, "Nije uneseno", ver,
                    KategorijaBodovanja.UDALJENOST);
        }

        double km = p.getUdaljenostKm().doubleValue();

        double b = km < 50  ? 0
                : km < 80  ? 4
                : km < 120 ? 8
                : km < 160 ? 12
                : 15;

        return new StavkaBodovanja("Udaljenost", b,
                String.format("%.1f km", km), ver,
                KategorijaBodovanja.UDALJENOST);
    }

    // ---------------- PRIMANJA ----------------

    /**
     * Ukupna primanja = zbir prosjecnih neto primanja svih clanova za zadnja 3 mjeseca.
     * Dijeli se s brojem clanova, ukljucujuci studenta.
     */
    private StavkaBodovanja bodoviPrimanja(Prijava p) {

        boolean ver = p.isPrimanjaVerifikovana();

        Integer clanova = p.getBrojClanovaDomacinstva();
        BigDecimal ukupno = p.getUkupnaPrimanja();

        if (ukupno == null || clanova == null || clanova < 1) {
            return new StavkaBodovanja("Primanja domaćinstva", 0,
                    "Nije uneseno", ver,
                    KategorijaBodovanja.PRIMANJA);
        }

        double poClanu = ukupno.doubleValue() / clanova;

        double b = poClanu < 110   ? 20
                : poClanu <= 150  ? 15
                : poClanu <= 200  ? 8
                : poClanu <= 300  ? 4
                : poClanu <= 400  ? 2
                : 0;

        return new StavkaBodovanja("Primanja domaćinstva", b,
                String.format("%.2f KM po članu (%d članova)", poClanu, clanova),
                ver,
                KategorijaBodovanja.PRIMANJA);
    }

    // ---------------- BRANIOCI ----------------

    private Optional<StavkaBodovanja> najveciBranilac(Prijava p,
                                                      boolean samoVerifikovani) {
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

        // po sifri - isti kriterij se broji samo jednom
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

    public StavkaBodovanja stavkaZaKriterij(PrijavaKriterij k) {

        VrstaKriterija v = k.getVrsta();

        // fiksni bodovi (parametar se samo prikazuje, npr. broj ratnih priznanja)
        if (v.getBodovi() != null) {
            String obr = v.getNazivParametra() != null && k.getParametar() != null
                    ? v.getNazivParametra() + ": " + k.getParametar().stripTrailingZeros().toPlainString()
                    : null;
            return new StavkaBodovanja(v.getNaziv(),
                    v.getBodovi().doubleValue(), obr, k.isVerifikovan(),
                    v.getGrupa());
        }

        // OS RBiH: period od-do, broje se samo puni mjeseci
        if ("DIJETE_OSRBIH".equals(v.getSifra())) {
            return stavkaOsRbih(k);
        }

        double param = k.getParametar() != null
                ? k.getParametar().doubleValue() : 0;

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

    /**
     * 0,3 boda po punom mjesecu ucesca. Ostatak dana se ne racuna
     * (6 mjeseci i 10 dana = 6 mjeseci; 8 mjeseci i 29 dana = 8 mjeseci).
     * Stare prijave bez datuma koriste broj mjeseci iz parametra.
     */
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

    /** Broj punih mjeseci između dva datuma; započeti mjesec se ne računa. */
    static long punihMjeseci(LocalDate od, LocalDate doDatuma) {
        if (od == null || doDatuma == null || doDatuma.isBefore(od)) return 0;
        return Period.between(od, doDatuma).toTotalMonths();
    }

    private double zaokruzi(double d) {
        return Math.round(d * 100.0) / 100.0;
    }
}