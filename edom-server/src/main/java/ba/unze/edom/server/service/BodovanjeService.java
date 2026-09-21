package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.RezultatBodovanja;
import ba.unze.edom.server.dto.StavkaBodovanja;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.entity.VrstaKriterija;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Racuna bodove prijave iz podataka same prijave.
 *
 * Bodovi se NIKAD ne akumuliraju u bazi - uvijek se racunaju iznova.
 * Zbog toga je svaka ispravka podataka odmah vidljiva u rezultatu,
 * a rezultat je uvijek objasnjiv kroz listu stavki.
 */
@Service
public class BodovanjeService {

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

        // --- branioci: najveci, odvojeno za potvrdjeno i potencijalno ---
        var braniociSvi         = najveciBranilac(p, false);
        var braniociPotvrdjeni  = najveciBranilac(p, true);

        // --- dodatni: zbrajaju se ---
        List<StavkaBodovanja> dodatni = dodatniKriteriji(p);

        // --- prikaz ---
        List<StavkaBodovanja> stavke = new ArrayList<>();
        stavke.add(uspjeh);
        stavke.add(udaljenost);
        stavke.add(primanja);
        braniociSvi.ifPresent(stavke::add);
        stavke.addAll(dodatni);

        // --- zbirovi ---
        List<StavkaBodovanja> osnovni = List.of(uspjeh, udaljenost, primanja);

        double potvrdjeno =
                zbir(osnovni, true)
                        + braniociPotvrdjeni.map(StavkaBodovanja::bodovi).orElse(0.0)
                        + zbir(dodatni, true);

        double potencijalno =
                zbir(osnovni, false)
                        + braniociSvi.map(StavkaBodovanja::bodovi).orElse(0.0)
                        + zbir(dodatni, false);

        boolean konacno = Statusi.ODOBRENO.equals(nazivStatusa(p));

        return new RezultatBodovanja(
                zaokruzi(potvrdjeno), zaokruzi(potencijalno), konacno, stavke);
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

    // ---------------- USPJEH ----------------

    private StavkaBodovanja bodoviUspjeh(Prijava p) {

        Integer godina = p.getGodinaStudija();
        BigDecimal prosjek = p.getProsjek();
        boolean ver = p.isUspjehVerifikovan();          // <-- flag iz prijave

        if (godina == null || prosjek == null) {
            return new StavkaBodovanja("Uspjeh", 0,
                    "Prosjek ili godina studija nisu uneseni", ver);
        }

        double pr = prosjek.doubleValue();

        if (godina == GodineStudija.PRVA) {
            double b = pr * 9 + 1;
            return new StavkaBodovanja("Uspjeh (brucoš)", zaokruzi(b),
                    "Prosjek " + pr, ver);
        }

        int ispiti = p.getPolozeniIspiti() != null ? p.getPolozeniIspiti() : 0;
        double b = pr * 3.5 + ispiti * 1.8 + bonusGodine(godina);

        return new StavkaBodovanja("Uspjeh", zaokruzi(b),
                "Prosjek " + pr + ", položenih ispita: " + ispiti
                        + ", " + GodineStudija.naziv(godina), ver);
    }

    private double bonusGodine(int godina) {
        return switch (godina) {
            case 2 -> 3;
            case 3 -> 5;
            case 4 -> 7;
            case 5 -> 9;
            case 6 -> 10;
            case GodineStudija.APSOLVENT -> 12;
            case GodineStudija.POSTDIPLOMAC -> 10;
            default -> 0;
        };
    }

    // ---------------- UDALJENOST ----------------

    private StavkaBodovanja bodoviUdaljenost(Prijava p) {

        boolean ver = p.isUdaljenostVerifikovana();

        if (p.getUdaljenostKm() == null) {
            return new StavkaBodovanja("Udaljenost", 0, "Nije uneseno", ver);
        }

        double km = p.getUdaljenostKm().doubleValue();

        double b = km < 50  ? 0
                : km < 80  ? 4
                : km < 120 ? 8
                : km < 160 ? 12
                : 15;

        return new StavkaBodovanja("Udaljenost", b,
                String.format("%.1f km", km), ver);
    }

    // ---------------- PRIMANJA ----------------

    private StavkaBodovanja bodoviPrimanja(Prijava p) {

        boolean ver = p.isPrimanjaVerifikovana();

        Integer clanova = p.getBrojClanovaDomacinstva();
        BigDecimal ukupno = p.getUkupnaPrimanja();

        if (ukupno == null || clanova == null || clanova < 1) {
            return new StavkaBodovanja("Primanja domaćinstva", 0,
                    "Nije uneseno", ver);
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
                ver);
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

    private StavkaBodovanja stavkaZaKriterij(PrijavaKriterij k) {

        VrstaKriterija v = k.getVrsta();

        if (v.getBodovi() != null) {
            return new StavkaBodovanja(v.getNaziv(),
                    v.getBodovi().doubleValue(), null, k.isVerifikovan());
        }

        double param = k.getParametar() != null
                ? k.getParametar().doubleValue() : 0;

        double b = switch (v.getSifra()) {
            case "STUDENT_RVI"       -> param == 0 ? 0 : (param < 60 ? 15 : 20);
            case "INVALID_RODITELJA" -> param * 0.1;
            case "DIJETE_OSRBIH"     -> param * 0.3;
            default -> 0;
        };

        String obrazlozenje = v.getNazivParametra() != null
                ? v.getNazivParametra() + ": " + param
                : null;

        return new StavkaBodovanja(v.getNaziv(), zaokruzi(b),
                obrazlozenje, k.isVerifikovan());
    }

    private double zaokruzi(double d) {
        return Math.round(d * 100.0) / 100.0;
    }
}