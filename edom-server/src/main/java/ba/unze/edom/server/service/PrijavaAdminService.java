package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.*;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.mapper.PrijaveMapper;
import ba.unze.edom.server.repository.DokumentRepository;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.StatusPrijaveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrijavaAdminService {

    /** Dozvoljeni prelazi statusa — jedino mjesto gdje su zapisani. */
    private static final Map<String, List<String>> PRELAZI = Map.of(
            Statusi.NA_PREGLEDU, List.of(Statusi.ODOBRENO, Statusi.ODBIJENO),
            Statusi.ODOBRENO,    List.of(Statusi.ZAKLJUCENO, Statusi.NA_PREGLEDU, Statusi.ODBIJENO),
            Statusi.ODBIJENO,    List.of(Statusi.NA_PREGLEDU),
            Statusi.ZAKLJUCENO,  List.of(),
            Statusi.U_IZRADI,    List.of()
    );

    private final PrijavaRepository prijavaRepository;
    private final DokumentRepository dokumentRepository;
    private final StatusPrijaveRepository statusRepository;
    private final BodovanjeService bodovanjeService;
    private final ApplicationEventPublisher dogadjaji;

    // ---------- LISTA ----------

    @Transactional(readOnly = true)
    public List<PrijavaListaDTO> lista(Integer godina) {
        Integer g = godina != null ? godina : prijavaRepository.zadnjaAkademskaGodina();
        if (g == null) return List.of();

        Map<Integer, Integer> brojDok = new HashMap<>();
        for (Object[] red : dokumentRepository.brojPoPrijavi(g)) {
            brojDok.put((Integer) red[0], ((Number) red[1]).intValue());
        }

        return prijavaRepository.zaAdminListu(g, Statusi.U_IZRADI).stream()
                .map(p -> uRed(p, brojDok.getOrDefault(p.getIdPrijava(), 0)))
                .toList();
    }

    private PrijavaListaDTO uRed(Prijava p, int brojDok) {
        try {
            return PrijaveMapper.uListu(p, bodovanjeService.izracunaj(p), null, brojDok);
        } catch (NevalidnaPrijavaException e) {
            return PrijaveMapper.uListu(p, null, e.getMessage(), brojDok);
        }
    }

    // ---------- DETALJI ----------

    @Transactional(readOnly = true)
    public PrijavaDetaljiDTO detalji(Integer idPrijave) {
        return izgradiDetalje(ucitaj(idPrijave));
    }

    // ---------- PROVJERA STAVKI ----------

    @Transactional
    public PrijavaDetaljiDTO verifikuj(Integer idPrijave, VerifikacijaDTO z) {
        Prijava p = ucitaj(idPrijave);

        if (!Statusi.NA_PREGLEDU.equals(nazivStatusa(p))) {
            throw new PrijavaException(
                    "Stavke se mogu provjeravati samo dok je prijava na pregledu. Vratite je prvo na pregled.");
        }

        p.setUspjehVerifikovan(z.uspjeh());
        p.setUdaljenostVerifikovana(z.udaljenost());
        p.setPrimanjaVerifikovana(z.primanja());

        Map<Integer, Boolean> poKriteriju = z.kriteriji() == null ? Map.of()
                : z.kriteriji().stream().collect(Collectors.toMap(
                KriterijVerifikacijaDTO::idPrijavaKriterij, KriterijVerifikacijaDTO::verifikovan));

        for (PrijavaKriterij k : p.getKriteriji()) {
            Boolean v = poKriteriju.get(k.getIdPrijavaKriterij());
            if (v != null) k.setVerifikovan(v);
        }

        osvjeziBodove(p);
        return izgradiDetalje(p);
    }

    // ---------- PROMJENA STATUSA ----------

    @Transactional
    public PrijavaDetaljiDTO promijeniStatus(Integer idPrijave, PromjenaStatusaDTO z) {
        Prijava p = ucitaj(idPrijave);
        String trenutni = nazivStatusa(p);
        String novi = z.status();

        if (!PRELAZI.getOrDefault(trenutni, List.of()).contains(novi)) {
            throw new PrijavaException(
                    "Prijava u statusu '" + trenutni + "' ne može preći u '" + novi + "'.");
        }

        String napomena = z.napomena() != null ? z.napomena().trim() : null;
        if (Statusi.ODBIJENO.equals(novi) && (napomena == null || napomena.isEmpty())) {
            throw new PrijavaException("Unesite razlog odbijanja — student ga dobija na e-mail.");
        }

        // odobriti se ne može prijava s nevalidnim podacima; izuzetak ide do korisnika
        RezultatBodovanja r = Statusi.ODOBRENO.equals(novi)
                ? bodovanjeService.izracunaj(p)
                : izracunajAkoMoze(p);

        p.setStatus(statusRepository.findByNaziv(novi)
                .orElseThrow(() -> new IllegalStateException("Status '" + novi + "' ne postoji u bazi.")));
        p.setNapomenaAdmina(napomena);
        if (r != null) p.setUkupniBodovi(r.potvrdjeno());

        if (Statusi.ODOBRENO.equals(novi) || Statusi.ODBIJENO.equals(novi)) {
            dogadjaji.publishEvent(new StatusPromijenjenEvent(
                    p.getStudent().getEmail(), p.getStudent().getIme(),
                    p.getAkademskaGodina(), novi, napomena, r));
        }

        return izgradiDetalje(p);
    }

    // ---------- SADRŽAJ DOKUMENTA ----------

    @Transactional(readOnly = true)
    public String sadrzajDokumenta(Integer idPrijave, Integer idDokumenta) {
        if (!dokumentRepository.existsByIdDokumentAndPrijava_IdPrijava(idDokumenta, idPrijave)) {
            throw new PrijavaException("Dokument ne pripada ovoj prijavi.");
        }
        return dokumentRepository.nadjiSadrzaj(idDokumenta)
                .orElseThrow(() -> new PrijavaException("Dokument nema priloženu datoteku."));
    }

    // ---------- pomoćne ----------

    private Prijava ucitaj(Integer idPrijave) {
        return prijavaRepository.zaDetalje(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));
    }

    private PrijavaDetaljiDTO izgradiDetalje(Prijava p) {
        RezultatBodovanja r = null;
        String greska = null;
        try {
            r = bodovanjeService.izracunaj(p);
        } catch (NevalidnaPrijavaException e) {
            greska = e.getMessage();
        }
        return PrijaveMapper.uDetalje(p, r, greska,
                dokumentRepository.pregledZaAdmina(p.getIdPrijava()),
                PRELAZI.getOrDefault(nazivStatusa(p), List.of()));
    }

    private void osvjeziBodove(Prijava p) {
        RezultatBodovanja r = izracunajAkoMoze(p);
        if (r != null) p.setUkupniBodovi(r.potvrdjeno());
    }

    private RezultatBodovanja izracunajAkoMoze(Prijava p) {
        try {
            return bodovanjeService.izracunaj(p);
        } catch (NevalidnaPrijavaException e) {
            return null;
        }
    }

    private String nazivStatusa(Prijava p) {
        return p.getStatus() != null ? p.getStatus().getNaziv() : null;
    }
}