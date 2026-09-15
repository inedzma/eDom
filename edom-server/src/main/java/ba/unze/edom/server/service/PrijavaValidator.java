package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PrijavaValidator {

    private static final BigDecimal NULA = BigDecimal.ZERO;
    private static final BigDecimal STO = BigDecimal.valueOf(100);

    private static final BigDecimal SREDNJA_MIN = BigDecimal.valueOf(1.0);
    private static final BigDecimal SREDNJA_MAX = BigDecimal.valueOf(5.0);
    private static final BigDecimal FAKULTET_MIN = BigDecimal.valueOf(6.0);
    private static final BigDecimal FAKULTET_MAX = BigDecimal.valueOf(10.0);

    private static final int MAX_UDALJENOST_KM = 1000;
    private static final int MAX_CLANOVA = 30;
    private static final int MAX_ISPITA = 100;
    private static final int MAX_MJESECI = 600;

    public void provjeri(Prijava p) {

        if (p == null) return;

        provjeriGodinuStudija(p);
        provjeriProsjek(p);
        provjeriIspite(p);
        provjeriUdaljenost(p);
        provjeriDomacinstvo(p);
        provjeriKriterije(p);
    }

    // ---------- godina studija ----------

    private void provjeriGodinuStudija(Prijava p) {
        Integer g = p.getGodinaStudija();
        if (g == null) return;

        if (!GodineStudija.validna(g)) {
            throw new NevalidnaPrijavaException(
                    "Godina studija mora biti između " + GodineStudija.MIN
                            + " i " + GodineStudija.MAX + ", a unesena je " + g + ".");
        }
    }

    // ---------- prosjek ----------

    private void provjeriProsjek(Prijava p) {
        BigDecimal pr = p.getProsjek();
        if (pr == null) return;

        if (pr.compareTo(NULA) < 0) {
            throw new NevalidnaPrijavaException(
                    "Prosjek ne može biti negativan.");
        }

        Integer g = p.getGodinaStudija();
        if (g == null) return;   // bez godine ne znamo koja skala vazi

        if (g == GodineStudija.PRVA) {
            if (pr.compareTo(SREDNJA_MIN) < 0 || pr.compareTo(SREDNJA_MAX) > 0) {
                throw new NevalidnaPrijavaException(
                        "Prosjek za prvu godinu mora biti između 1.00 i 5.00 "
                                + "(skala srednje škole), a unesen je " + pr + ".");
            }
        } else {
            if (pr.compareTo(FAKULTET_MIN) < 0 || pr.compareTo(FAKULTET_MAX) > 0) {
                throw new NevalidnaPrijavaException(
                        "Prosjek mora biti između 6.00 i 10.00, "
                                + "a unesen je " + pr + ".");
            }
        }
    }

    // ---------- polozeni ispiti ----------

    private void provjeriIspite(Prijava p) {
        Integer i = p.getPolozeniIspiti();
        if (i == null) return;

        if (i < 0) {
            throw new NevalidnaPrijavaException(
                    "Broj položenih ispita ne može biti negativan.");
        }
        if (i > MAX_ISPITA) {
            throw new NevalidnaPrijavaException(
                    "Broj položenih ispita (" + i + ") je nerealno velik.");
        }
    }

    // ---------- udaljenost ----------

    private void provjeriUdaljenost(Prijava p) {
        BigDecimal km = p.getUdaljenostKm();
        if (km == null) return;

        if (km.compareTo(NULA) < 0) {
            throw new NevalidnaPrijavaException(
                    "Udaljenost ne može biti negativna.");
        }
        if (km.compareTo(BigDecimal.valueOf(MAX_UDALJENOST_KM)) > 0) {
            throw new NevalidnaPrijavaException(
                    "Udaljenost od " + km + " km je nerealna.");
        }
    }

    // ---------- domacinstvo ----------

    private void provjeriDomacinstvo(Prijava p) {

        Integer clanova = p.getBrojClanovaDomacinstva();

        if (clanova != null) {
            if (clanova < 1) {
                throw new NevalidnaPrijavaException(
                        "Domaćinstvo mora imati najmanje jednog člana.");
            }
            if (clanova > MAX_CLANOVA) {
                throw new NevalidnaPrijavaException(
                        "Broj članova domaćinstva (" + clanova + ") je nerealan.");
            }
        }

        BigDecimal primanja = p.getUkupnaPrimanja();
        if (primanja != null && primanja.compareTo(NULA) < 0) {
            throw new NevalidnaPrijavaException(
                    "Primanja domaćinstva ne mogu biti negativna.");
        }

        // primanja bez broja clanova se ne mogu podijeliti
        if (primanja != null && clanova == null) {
            throw new NevalidnaPrijavaException(
                    "Unesena su primanja, ali nije unesen broj članova domaćinstva.");
        }
    }

    // ---------- kriteriji ----------

    private void provjeriKriterije(Prijava p) {

        if (p.getKriteriji() == null) return;

        for (PrijavaKriterij k : p.getKriteriji()) {

            if (k.getVrsta() == null) {
                throw new NevalidnaPrijavaException(
                        "Kriterij bez definisane vrste.");
            }

            BigDecimal param = k.getParametar();

            if (k.getVrsta().isTraziParametar() && param != null) {

                if (param.compareTo(NULA) < 0) {
                    throw new NevalidnaPrijavaException(
                            "Parametar kriterija '" + k.getVrsta().getNaziv()
                                    + "' ne može biti negativan.");
                }

                boolean postotak = k.getVrsta().getSifra().equals("STUDENT_RVI")
                        || k.getVrsta().getSifra().equals("INVALID_RODITELJA");

                if (postotak && param.compareTo(STO) > 0) {
                    throw new NevalidnaPrijavaException(
                            "Postotak invalidnosti ne može biti veći od 100, "
                                    + "a unesen je " + param + ".");
                }

                if (k.getVrsta().getSifra().equals("DIJETE_OSRBIH")
                        && param.compareTo(BigDecimal.valueOf(MAX_MJESECI)) > 0) {
                    throw new NevalidnaPrijavaException(
                            "Broj mjeseci učešća (" + param + ") je nerealan.");
                }
            }

            // parametarski kriterij bez parametra nije greska -
            // znaci da student jos nije unio vrijednost
        }
    }
}