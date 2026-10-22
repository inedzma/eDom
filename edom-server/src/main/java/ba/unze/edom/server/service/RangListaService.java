package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.KategorijaBodovanja;
import ba.unze.edom.common.dto.KategorijaRanga;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RangListaService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final PrijavaRepository prijavaRepository;
    private final BodovanjeService bodovanjeService;
    private final RangRaspodjela raspodjela;

    @Value("${edom.rang.kapacitet:220}")
    private int kapacitetPoDefaultu;

    @Value("${edom.rang.udio-brucosa:0.35}")
    private double udioBrucosa;

    // ---------- interni model ----------

    private record Rangirana(Prijava prijava, RezultatBodovanja rezultat, String greska) {
        double bodovi() {
            return rezultat != null ? rezultat.potvrdjeno() : 0.0;
        }
    }

    // ---------- JAVNO (web) ----------

    @Transactional(readOnly = true)
    public RangListaDTO trenutna() {
        Integer godina = prijavaRepository.zadnjaAkademskaGodina();
        if (godina == null) {
            return new RangListaDTO(null, List.of(), List.of());
        }
        return zaGodinu(godina);
    }

    /** Isti redoslijed kao na desktopu; viši: prvo s uslovom, pa lista čekanja. */
    @Transactional(readOnly = true)
    public RangListaDTO zaGodinu(Integer godina) {
        RangListaAdminDTO a = izracunaj(godina, kapacitetPoDefaultu, false);
        List<RangRedDTO> visi = new ArrayList<>(a.saUslovom());
        visi.addAll(a.listaCekanja());
        return new RangListaDTO(a.akademskaGodina(), javno(a.brucosi()), javno(visi));
    }

    // ---------- ADMIN (desktop) ----------

    @Transactional(readOnly = true)
    public RangListaAdminDTO zaAdmina(Integer godina, Integer kapacitet, boolean popuniBrucosima) {
        Integer g = godina != null ? godina : prijavaRepository.zadnjaAkademskaGodina();
        int kap = kapacitet != null && kapacitet > 0 ? kapacitet : kapacitetPoDefaultu;
        if (g == null) {
            return raspodjela.rasporedi(null, null, List.of(), kap, udioBrucosa, popuniBrucosima);
        }
        return izracunaj(g, kap, popuniBrucosima);
    }

    // ---------- rangiranje ----------

    private RangListaAdminDTO izracunaj(Integer godina, int kapacitet, boolean popuniBrucosima) {
        List<RangRedDTO> redovi = prijavaRepository.rangLista(godina).stream()
                .filter(this::jeOdobrena)
                .map(this::boduj)
                .map(this::uRed)
                .toList();
        return raspodjela.rasporedi(naziv(godina), godina, redovi, kapacitet, udioBrucosa, popuniBrucosima);
    }

    private Rangirana boduj(Prijava p) {
        try {
            return new Rangirana(p, bodovanjeService.izracunaj(p), null);
        } catch (NevalidnaPrijavaException e) {
            return new Rangirana(p, null, e.getMessage());
        }
    }

    // ---------- mapiranje ----------

    /** Javna lista: prijave s neispravnim podacima se ne objavljuju. */
    private List<RangStavkaDTO> javno(List<RangRedDTO> lista) {
        return lista.stream()
                .filter(r -> r.greska() == null)
                .map(r -> new RangStavkaDTO(r.idStudent(), r.prezime(), r.imeOca(), r.ime(), r.ukupno()))
                .toList();
    }

    private RangRedDTO uRed(Rangirana r) {
        Prijava p = r.prijava();
        Student s = p.getStudent();

        double uspjeh     = potvrdjeno(r, KategorijaBodovanja.USPJEH);
        double udaljenost = potvrdjeno(r, KategorijaBodovanja.UDALJENOST);
        double socijalni  = zaokruzi(potvrdjeno(r, KategorijaBodovanja.PRIMANJA)
                + potvrdjeno(r, GrupeKriterija.DODATNI));
        double ukupno     = r.bodovi();

        // branioci: uzima se samo najveći potvrđeni, pa se računa kao ostatak do ukupnog
        double branioci = r.rezultat() == null ? 0
                : zaokruzi(ukupno - uspjeh - udaljenost - socijalni);

        return new RangRedDTO(
                0,                                   // redni broj dodjeljuje RangRaspodjela
                p.getIdPrijava(), s.getIdStudent(),
                SifraStudenta.format(s.getSifraBroj(), p.getAkademskaGodina()),
                s.getPrezime(), s.getImeRoditelja(), s.getIme(),
                godinaStudija(p),
                kategorija(p),
                uspjeh, branioci, socijalni, udaljenost, ukupno,
                false,                               // primljen: računa RangRaspodjela
                p.getDokazUslovaAt() == null ? null : p.getDokazUslovaAt().format(FMT),
                p.getDokazUslovaEvidentirao() != null,
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

    private KategorijaRanga kategorija(Prijava p) {
        if (jeBrucos(p)) return KategorijaRanga.BRUCOS;
        String s = p.getStatus() != null ? p.getStatus().getNaziv() : null;
        return Statusi.NA_CEKANJU.equals(s) ? KategorijaRanga.LISTA_CEKANJA : KategorijaRanga.SA_USLOVOM;
    }
    private boolean jeOdobrena(Prijava p) {
        String s = p.getStatus() != null ? p.getStatus().getNaziv() : null;
        return Statusi.ODOBRENO.equalsIgnoreCase(s)
                || Statusi.ZAKLJUCENO.equalsIgnoreCase(s)
                || Statusi.NA_CEKANJU.equalsIgnoreCase(s);
    }

    private Integer godinaStudija(Prijava p) {
        return UslovUpisa.godinaStudija(p);
    }

    private boolean jeBrucos(Prijava p) {
        return UslovUpisa.jeBrucos(p);
    }

    private String naziv(Integer godina) {
        return godina + "/" + (godina + 1);
    }

    private double zaokruzi(double d) {
        return Math.round(d * 100.0) / 100.0;
    }


}