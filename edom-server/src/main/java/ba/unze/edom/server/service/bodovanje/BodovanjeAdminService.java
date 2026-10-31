package ba.unze.edom.server.service.bodovanje;

import ba.unze.edom.common.dto.bodovanje.*;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.entity.VrstaKriterija;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.VrstaKriterijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** Tabela bodovanja u detaljima prijave: svi kriteriji, izmjena podataka, provjera. */
@Service
@RequiredArgsConstructor
public class BodovanjeAdminService {

    private final PrijavaRepository prijavaRepository;
    private final VrstaKriterijaRepository vrstaKriterijaRepository;
    private final BodovanjeService bodovanjeService;

    @Transactional(readOnly = true)
    public BodovanjeDetaljnoDTO detaljno(Integer idPrijave) {
        return dto(ucitaj(idPrijave));
    }

    /** Admin smije mijenjati u svakom statusu; bodovi se odmah preračunaju. */
    @Transactional
    public BodovanjeDetaljnoDTO sacuvaj(Integer idPrijave, BodovanjeIzmjenaDTO z) {
        Prijava p = ucitaj(idPrijave);

        p.setGodinaStudija(z.godinaStudija());
        p.setUpisGodinePut(z.upisGodinePut() != null ? z.upisGodinePut() : 1);
        p.setProsjek(z.prosjek());
        p.setPolozeniIspiti(z.polozeniIspiti());
        p.setUdaljenostKm(z.udaljenostKm());
        p.setBrojClanovaDomacinstva(z.brojClanova());
        p.setUkupnaPrimanja(z.ukupnaPrimanja());

        p.setUspjehVerifikovan(z.uspjehVerifikovan());
        p.setUdaljenostVerifikovana(z.udaljenostVerifikovana());
        p.setPrimanjaVerifikovana(z.primanjaVerifikovana());

        sinhronizujKriterije(p, z.kriteriji());

        try {
            p.setUkupniBodovi(bodovanjeService.izracunaj(p).potvrdjeno());
        } catch (NevalidnaPrijavaException e) {
            throw new PrijavaException(e.getMessage());   // ništa se ne snima
        }
        return dto(p);
    }

    // ---------- kriteriji ----------

    private void sinhronizujKriterije(Prijava p, List<BodovanjeKriterijIzmjenaDTO> lista) {
        Map<Integer, BodovanjeKriterijIzmjenaDTO> trazeni = (lista != null ? lista : List.<BodovanjeKriterijIzmjenaDTO>of())
                .stream()
                .collect(Collectors.toMap(BodovanjeKriterijIzmjenaDTO::idVrste, k -> k, (a, b) -> a, LinkedHashMap::new));

        // neoznačeni se brišu
        p.getKriteriji().removeIf(k -> k.getVrsta() == null
                || !trazeni.containsKey(k.getVrsta().getIdVrstaKriterija()));

        Map<Integer, PrijavaKriterij> postojeci = p.getKriteriji().stream()
                .collect(Collectors.toMap(k -> k.getVrsta().getIdVrstaKriterija(), k -> k, (a, b) -> a));

        for (BodovanjeKriterijIzmjenaDTO z : trazeni.values()) {
            PrijavaKriterij k = postojeci.get(z.idVrste());
            VrstaKriterija v;
            if (k == null) {
                v = vrstaKriterijaRepository.findById(z.idVrste())
                        .orElseThrow(() -> new PrijavaException("Nepoznat kriterij."));
                k = new PrijavaKriterij();
                k.setVrsta(v);
                p.dodajKriterij(k);
            } else {
                v = k.getVrsta();
            }

            if (v.isTraziParametar() && z.parametar() == null) {
                throw new PrijavaException("Za kriterij '" + v.getNaziv() + "' unesite "
                        + (v.getNazivParametra() != null ? v.getNazivParametra().toLowerCase() : "vrijednost") + ".");
            }
            k.setParametar(v.isTraziParametar() ? z.parametar() : null);

            if (v.isTraziPeriod()) {
                if (z.datumOd() == null || z.datumDo() == null) {
                    throw new PrijavaException("Za kriterij '" + v.getNaziv() + "' unesite datum od i datum do.");
                }
                if (z.datumOd().isAfter(z.datumDo())) {
                    throw new PrijavaException("Za kriterij '" + v.getNaziv() + "' datum od je poslije datuma do.");
                }
                k.setDatumOd(z.datumOd());
                k.setDatumDo(z.datumDo());
            }

            k.setVerifikovan(z.verifikovan());
        }
    }

    // ---------- prikaz ----------

    private BodovanjeDetaljnoDTO dto(Prijava p) {
        RezultatBodovanja r = null;
        String greska = null;
        try {
            r = bodovanjeService.izracunaj(p);
        } catch (NevalidnaPrijavaException e) {
            greska = e.getMessage();
        }

        List<StavkaBodovanja> osnovne = r == null ? List.of() : r.stavke().stream()
                .filter(s -> KategorijaBodovanja.USPJEH.equals(s.kategorija())
                        || KategorijaBodovanja.UDALJENOST.equals(s.kategorija())
                        || KategorijaBodovanja.PRIMANJA.equals(s.kategorija()))
                .toList();

        // svi aktivni kriteriji + eventualno označeni neaktivni
        List<VrstaKriterija> sve = new ArrayList<>(vrstaKriterijaRepository.findByAktivanTrueOrderByGrupaAscNazivAsc());
        Map<Integer, PrijavaKriterij> izabrani = p.getKriteriji().stream()
                .filter(k -> k.getVrsta() != null)
                .collect(Collectors.toMap(k -> k.getVrsta().getIdVrstaKriterija(), k -> k, (a, b) -> a));
        Set<Integer> uListi = sve.stream().map(VrstaKriterija::getIdVrstaKriterija).collect(Collectors.toSet());
        izabrani.values().stream()
                .map(PrijavaKriterij::getVrsta)
                .filter(v -> !uListi.contains(v.getIdVrstaKriterija()))
                .forEach(sve::add);

        Map<Integer, Double> bodovi = new HashMap<>();
        izabrani.forEach((id, k) -> bodovi.put(id, bodovanjeService.stavkaZaKriterij(k).bodovi()));

        // koji branilac ulazi u zbir (najveći)
        Integer najveciBranilac = izabrani.entrySet().stream()
                .filter(e -> GrupeKriterija.BRANIOCI.equals(e.getValue().getVrsta().getGrupa()))
                .max(Comparator.comparingDouble(e -> bodovi.get(e.getKey())))
                .map(Map.Entry::getKey)
                .orElse(null);

        List<BodovanjeKriterijDTO> kriteriji = sve.stream().map(v -> {
            PrijavaKriterij k = izabrani.get(v.getIdVrstaKriterija());
            boolean izabran = k != null;
            return new BodovanjeKriterijDTO(
                    v.getIdVrstaKriterija(), v.getSifra(), v.getNaziv(), v.getGrupa(), pravilo(v),
                    v.isTraziParametar(), v.getNazivParametra(), v.isTraziPeriod(),
                    izabran,
                    izabran ? k.getParametar() : null,
                    izabran ? k.getDatumOd() : null,
                    izabran ? k.getDatumDo() : null,
                    izabran && k.isVerifikovan(),
                    izabran ? bodovi.get(v.getIdVrstaKriterija()) : 0,
                    Objects.equals(najveciBranilac, v.getIdVrstaKriterija()));
        }).toList();

        return new BodovanjeDetaljnoDTO(
                p.getIdPrijava(),
                p.getGodinaStudija(), p.getUpisGodinePut(), p.getProsjek(), p.getPolozeniIspiti(),
                p.getUdaljenostKm(), p.getBrojClanovaDomacinstva(), p.getUkupnaPrimanja(),
                p.isUspjehVerifikovan(), p.isUdaljenostVerifikovana(), p.isPrimanjaVerifikovana(),
                osnovne, kriteriji,
                r != null ? r.potvrdjeno() : 0,
                r != null ? r.potencijalno() : 0,
                r != null && r.upozorenja() != null ? r.upozorenja() : List.of(),
                greska);
    }

    /** Koliko kriterij nosi — za kolonu "Pravilo". */
    private String pravilo(VrstaKriterija v) {
        if (v.getBodovi() != null) {
            return v.getBodovi().stripTrailingZeros().toPlainString() + " bod.";
        }
        return switch (v.getSifra()) {
            case "STUDENT_RVI" -> "do 60%: 15 · 60% i više: 20";
            case "INVALID_RODITELJA" -> "% invalidnosti × 0,1";
            case "DIJETE_OSRBIH" -> "puni mjeseci × 0,3";
            default -> "—";
        };
    }

    private Prijava ucitaj(Integer idPrijave) {
        return prijavaRepository.zaDetalje(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));
    }
}