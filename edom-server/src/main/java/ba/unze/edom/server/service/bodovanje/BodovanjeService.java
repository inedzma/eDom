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

    private final PrijavaValidator validator;
    private final PostavkeService postavke;

    public BodovanjeService(PrijavaValidator validator, PostavkeService postavke) {
        this.validator = validator;
        this.postavke = postavke;
    }

    public RezultatBodovanja izracunaj(Prijava p) {

        if (p == null) {
            return new RezultatBodovanja(0, false, List.of(), List.of());
        }

        validator.provjeri(p);
        PravilaBodovanja pr = postavke.pravila();

        List<StavkaBodovanja> stavke = new ArrayList<>();
        stavke.add(bodoviUspjeh(p, pr));
        kaznaPonovniUpis(p, pr).ifPresent(stavke::add);
        stavke.add(bodoviUdaljenost(p, pr));
        stavke.add(bodoviPrimanja(p, pr));
        najveciBranilac(p).ifPresent(stavke::add);
        stavke.addAll(dodatniKriteriji(p));

        double ukupno = stavke.stream().mapToDouble(StavkaBodovanja::bodovi).sum();

        String st = p.getStatus() != null ? p.getStatus().getNaziv() : null;
        boolean konacno = Statusi.ODOBRENO.equals(st) || Statusi.ZAKLJUCENO.equals(st);

        return new RezultatBodovanja(zaokruzi(ukupno), konacno, stavke, upozorenja(p));
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

        if (godina == null || prosjek == null) {
            return new StavkaBodovanja("Uspjeh", 0,
                    "Prosjek ili godina studija nisu uneseni", KategorijaBodovanja.USPJEH);
        }

        double prs = prosjek.doubleValue();

        if (godina == GodineStudija.PRVA) {
            double b = prs * pr.brucosMnozilac() + pr.brucosDodatak();
            return new StavkaBodovanja("Uspjeh (brucoš)", zaokruzi(b),
                    "Prosjek " + prs, KategorijaBodovanja.USPJEH);
        }

        int ispiti = p.getPolozeniIspiti() != null ? p.getPolozeniIspiti() : 0;
        double b = prs * pr.prosjekMnozilac() + ispiti * pr.ispitMnozilac() + pr.bonus(godina);

        return new StavkaBodovanja("Uspjeh", zaokruzi(b),
                "Prosjek " + prs + ", položenih ispita: " + ispiti + ", " + GodineStudija.naziv(godina),
                KategorijaBodovanja.USPJEH);
    }

    /** Koristi ga test; stvarne vrijednosti su u Postavkama. */
    double bonusGodine(int godina) {
        return postavke.pravila().bonus(godina);
    }

    private Optional<StavkaBodovanja> kaznaPonovniUpis(Prijava p, PravilaBodovanja pr) {
        Integer put = p.getUpisGodinePut();
        if (put == null || put < 2 || pr.kaznaPonovniUpis() == 0) return Optional.empty();

        return Optional.of(new StavkaBodovanja("Ponovni upis godine", pr.kaznaPonovniUpis(),
                "Tekuću godinu upisuje " + put + ". put", KategorijaBodovanja.USPJEH));
    }

    // ---------------- UDALJENOST ----------------

    private StavkaBodovanja bodoviUdaljenost(Prijava p, PravilaBodovanja pr) {
        if (p.getUdaljenostKm() == null) {
            return new StavkaBodovanja("Udaljenost", 0, "Nije uneseno", KategorijaBodovanja.UDALJENOST);
        }
        double km = p.getUdaljenostKm().doubleValue();
        return new StavkaBodovanja("Udaljenost", pr.udaljenost().bodovi(km),
                String.format("%.1f km", km), KategorijaBodovanja.UDALJENOST);
    }

    // ---------------- PRIMANJA ----------------

    private StavkaBodovanja bodoviPrimanja(Prijava p, PravilaBodovanja pr) {
        Integer clanova = p.getBrojClanovaDomacinstva();
        BigDecimal ukupno = p.getUkupnaPrimanja();

        if (ukupno == null || clanova == null || clanova < 1) {
            return new StavkaBodovanja("Primanja domaćinstva", 0, "Nije uneseno", KategorijaBodovanja.PRIMANJA);
        }

        double poClanu = ukupno.doubleValue() / clanova;
        return new StavkaBodovanja("Primanja domaćinstva", pr.primanja().bodovi(poClanu),
                String.format("%.2f KM po članu (%d članova)", poClanu, clanova),
                KategorijaBodovanja.PRIMANJA);
    }

    // ---------------- BRANIOCI ----------------

    private Optional<StavkaBodovanja> najveciBranilac(Prijava p) {
        if (p.getKriteriji() == null) return Optional.empty();

        return p.getKriteriji().stream()
                .filter(k -> k.getVrsta() != null)
                .filter(k -> GrupeKriterija.BRANIOCI.equals(k.getVrsta().getGrupa()))
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

    /**
     * Stavka za jedan kriterij. Koristi je i admin pregled (BodovanjeAdminService).
     * Fiksni bodovi iz tabele, osim tri kriterija čije bodove Pravilnik računa iz parametra.
     */
    public StavkaBodovanja stavkaZaKriterij(PrijavaKriterij k) {

        VrstaKriterija v = k.getVrsta();

        String obr = v.getNazivParametra() != null && k.getParametar() != null
                ? v.getNazivParametra() + ": " + k.getParametar().stripTrailingZeros().toPlainString()
                : null;

        if (v.getBodovi() != null) {
            return new StavkaBodovanja(v.getNaziv(), v.getBodovi().doubleValue(), obr, v.getGrupa());
        }

        double param = k.getParametar() != null ? k.getParametar().doubleValue() : 0;

        double b = switch (v.getSifra()) {
            case "STUDENT_RVI"       -> param == 0 ? 0 : (param < 60 ? 15 : 20);
            case "INVALID_RODITELJA" -> param * 0.1;
            case "DIJETE_OSRBIH"     -> Math.floor(param) * 0.3;   // parametar = broj punih mjeseci
            default -> 0;
        };

        return new StavkaBodovanja(v.getNaziv(), zaokruzi(b), obr, v.getGrupa());
    }

    private double zaokruzi(double d) {
        return Math.round(d * 100.0) / 100.0;
    }
}