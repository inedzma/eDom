package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.KategorijaBodovanja;
import ba.unze.edom.common.dto.RangListaAdminDTO;
import ba.unze.edom.common.dto.RangRedDTO;
import ba.unze.edom.common.dto.RezultatBodovanja;
import ba.unze.edom.common.dto.StavkaBodovanja;
import ba.unze.edom.server.dto.RangListaDTO;
import ba.unze.edom.server.dto.RangStavkaDTO;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import ba.unze.edom.server.repository.PrijavaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RangListaService {

    private final PrijavaRepository prijavaRepository;
    private final BodovanjeService bodovanjeService;

    // ---------- interni model ----------

    /** Jedna rangirana prijava; iz nje nastaju i javni i admin prikaz. */
    private record Rangirana(Prijava prijava, RezultatBodovanja rezultat, String greska) {
        double bodovi() {
            return rezultat != null ? rezultat.potvrdjeno() : 0.0;
        }
    }

    private record Podjela(List<Rangirana> brucosi, List<Rangirana> visegodisnji) {}

    /**
     * Najviše potvrđenih bodova prvo.
     * TODO: pravilo za izjednačene bodove prema Pravilniku (pitanje za Studentski centar).
     */
    private static final Comparator<Rangirana> PORADAK = Comparator
            .comparingDouble(Rangirana::bodovi).reversed()
            .thenComparing(r -> r.prijava().getStudent().getPrezime(),
                    Comparator.nullsLast(String::compareToIgnoreCase));

    // ---------- JAVNO (web) ----------

    @Transactional(readOnly = true)
    public RangListaDTO trenutna() {
        Integer godina = prijavaRepository.zadnjaAkademskaGodina();
        if (godina == null) {
            return new RangListaDTO(null, List.of(), List.of());
        }
        return zaGodinu(godina);
    }

    @Transactional(readOnly = true)
    public RangListaDTO zaGodinu(Integer godina) {
        Podjela d = rangiraj(godina);
        return new RangListaDTO(naziv(godina), javno(d.brucosi()), javno(d.visegodisnji()));
    }

    // ---------- ADMIN (desktop) ----------

    @Transactional(readOnly = true)
    public RangListaAdminDTO zaAdmina(Integer godina) {
        Integer g = godina != null ? godina : prijavaRepository.zadnjaAkademskaGodina();
        if (g == null) {
            return new RangListaAdminDTO(null, null, List.of(), List.of());
        }
        Podjela d = rangiraj(g);
        return new RangListaAdminDTO(naziv(g), g, admin(d.brucosi()), admin(d.visegodisnji()));
    }

    // ---------- rangiranje ----------

    private Podjela rangiraj(Integer godina) {
        List<Rangirana> brucosi = new ArrayList<>();
        List<Rangirana> visegodisnji = new ArrayList<>();

        for (Prijava p : prijavaRepository.rangLista(godina)) {
            if (!jeOdobrena(p)) continue;
            Rangirana r = izracunaj(p);
            (jeBrucos(p) ? brucosi : visegodisnji).add(r);
        }

        brucosi.sort(PORADAK);
        visegodisnji.sort(PORADAK);
        return new Podjela(brucosi, visegodisnji);
    }

    private Rangirana izracunaj(Prijava p) {
        try {
            return new Rangirana(p, bodovanjeService.izracunaj(p), null);
        } catch (NevalidnaPrijavaException e) {
            return new Rangirana(p, null, e.getMessage());
        }
    }

    // ---------- mapiranje ----------

    /** Javna lista: prijave s neispravnim podacima se ne objavljuju. */
    private List<RangStavkaDTO> javno(List<Rangirana> lista) {
        return lista.stream()
                .filter(r -> r.greska() == null)
                .map(r -> {
                    Student s = r.prijava().getStudent();
                    return new RangStavkaDTO(s.getIdStudent(), s.getPrezime(),
                            s.getImeRoditelja(), s.getIme(), r.bodovi());
                })
                .toList();
    }

    private List<RangRedDTO> admin(List<Rangirana> lista) {
        return IntStream.range(0, lista.size())
                .mapToObj(i -> uRed(i + 1, lista.get(i)))
                .toList();
    }

    private RangRedDTO uRed(int redniBroj, Rangirana r) {
        Prijava p = r.prijava();
        Student s = p.getStudent();

        double uspjeh     = potvrdjeno(r, KategorijaBodovanja.USPJEH);
        double udaljenost = potvrdjeno(r, KategorijaBodovanja.UDALJENOST);
        double primanja   = potvrdjeno(r, KategorijaBodovanja.PRIMANJA);
        double dodatni    = potvrdjeno(r, GrupeKriterija.DODATNI);
        double ukupno     = r.bodovi();

        // branioci: uzima se samo najveći potvrđeni, pa se računa kao ostatak do ukupnog
        double branioci = r.rezultat() == null ? 0
                : zaokruzi(ukupno - uspjeh - udaljenost - primanja - dodatni);

        return new RangRedDTO(
                redniBroj, p.getIdPrijava(), s.getIdStudent(),
                s.getPrezime(), s.getImeRoditelja(), s.getIme(),
                godinaStudija(p),
                uspjeh, udaljenost, primanja, branioci, dodatni, ukupno,
                r.greska());
    }

    private double potvrdjeno(Rangirana r, String kategorija) {
        if (r.rezultat() == null) return 0;
        return zaokruzi(r.rezultat().stavke().stream()
                .filter(st -> st.verifikovan() && kategorija.equals(st.kategorija()))
                .mapToDouble(StavkaBodovanja::bodovi)
                .sum());
    }

    // ---------- pomoćne ----------

    private boolean jeOdobrena(Prijava p) {
        String s = p.getStatus() != null ? p.getStatus().getNaziv() : null;
        return Statusi.ODOBRENO.equalsIgnoreCase(s) || Statusi.ZAKLJUCENO.equalsIgnoreCase(s);
    }

    /** Godina iz prijave ima prednost; ako je nema, uzima se iz profila studenta. */
    private Integer godinaStudija(Prijava p) {
        return p.getGodinaStudija() != null ? p.getGodinaStudija() : p.getStudent().getGodinaStudija();
    }

    private boolean jeBrucos(Prijava p) {
        Integer g = godinaStudija(p);
        return g != null && g == GodineStudija.PRVA;
    }

    private String naziv(Integer godina) {
        return godina + "/" + (godina + 1);
    }

    private double zaokruzi(double d) {
        return Math.round(d * 100.0) / 100.0;
    }
}