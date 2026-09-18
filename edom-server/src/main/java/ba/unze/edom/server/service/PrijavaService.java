package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.*;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PrijavaService {

    private final PrijavaRepository prijavaRepository;
    private final StudentRepository studentRepository;
    private final StatusPrijaveRepository statusRepository;
    private final VrstaKriterijaRepository vrstaKriterijaRepository;
    private final BodovanjeService bodovanjeService;

    // ---------- KREIRANJE ----------

    @Transactional
    public Prijava kreirajIliNastavi(Integer idStudenta, int akademskaGodina) {

        var postojeca = prijavaRepository
                .findByStudent_IdStudentAndAkademskaGodina(idStudenta, akademskaGodina);

        if (postojeca.isPresent()) {
            Prijava p = postojeca.get();
            if (!Statusi.U_IZRADI.equals(nazivStatusa(p))) {
                throw new PrijavaException(
                        "Već ste podnijeli prijavu za akademsku " + akademskaGodina
                                + ". godinu. Za izmjene se obratite administraciji doma.");
            }
            return p;   // nastavak zapocete prijave
        }

        Student student = studentRepository.findById(idStudenta)
                .orElseThrow(() -> new PrijavaException("Student ne postoji."));

        Prijava p = new Prijava();
        p.setStudent(student);
        p.setAkademskaGodina(akademskaGodina);
        p.setDatumPrijave(LocalDate.now());
        p.setStatus(status(Statusi.U_IZRADI));
        p.setUkupniBodovi(0.0);

        // pocetne vrijednosti iz profila - student ih moze promijeniti
        p.setGodinaStudija(student.getGodinaStudija());

        return prijavaRepository.save(p);
    }

    // ---------- KORAK 1 ----------

    @Transactional
    public void sacuvajStudij(Integer idPrijave, Integer idStudenta,
                              StudijKorakDTO dto) {
        Prijava p = dohvatiZaIzmjenu(idPrijave, idStudenta);

        p.setAkademskaGodina(dto.akademskaGodina());
        p.setGodinaStudija(dto.godinaStudija());
        p.setProsjek(dto.prosjek());
        p.setPolozeniIspiti(dto.polozeniIspiti());
    }

    // ---------- KORAK 2 ----------

    @Transactional
    public void sacuvajDomacinstvo(Integer idPrijave, Integer idStudenta,
                                   DomacinstvoKorakDTO dto) {
        Prijava p = dohvatiZaIzmjenu(idPrijave, idStudenta);

        p.setBrojClanovaDomacinstva(dto.brojClanovaDomacinstva());
        p.setUkupnaPrimanja(dto.ukupnaPrimanja());
        p.setUdaljenostKm(dto.udaljenostKm());
    }

    // ---------- KORAK 3 ----------

    /** Priprema formu - sve aktivne vrste, sa oznakom sta je vec izabrano. */
    @Transactional(readOnly = true)
    public KriterijiFormaDTO pripremiKriterije(Integer idPrijave, Integer idStudenta) {

        Prijava p = dohvatiZaIzmjenu(idPrijave, idStudenta);

        Map<Integer, PrijavaKriterij> izabrani = new HashMap<>();
        for (PrijavaKriterij k : p.getKriteriji()) {
            izabrani.put(k.getVrsta().getIdVrstaKriterija(), k);
        }

        var forma = new KriterijiFormaDTO();

        for (VrstaKriterija v : vrstaKriterijaRepository
                .findByAktivanTrueOrderByGrupaAscNazivAsc()) {

            var red = new KriterijIzborDTO();
            red.setIdVrsteKriterija(v.getIdVrstaKriterija());
            red.setNaziv(v.getNaziv());
            red.setGrupa(v.getGrupa());
            red.setTraziParametar(v.isTraziParametar());
            red.setNazivParametra(v.getNazivParametra());

            PrijavaKriterij vec = izabrani.get(v.getIdVrstaKriterija());
            if (vec != null) {
                red.setIzabran(true);
                red.setParametar(vec.getParametar());
            }

            forma.getKriteriji().add(red);
        }

        return forma;
    }

    @Transactional
    public void sacuvajKriterije(Integer idPrijave, Integer idStudenta,
                                 KriterijiFormaDTO forma) {

        Prijava p = dohvatiZaIzmjenu(idPrijave, idStudenta);

        // izmjena kriterija ponistava raniju verifikaciju
        p.getKriteriji().clear();

        prijavaRepository.saveAndFlush(p);
        for (KriterijIzborDTO red : forma.getKriteriji()) {

            if (!red.isIzabran()) continue;

            VrstaKriterija v = vrstaKriterijaRepository
                    .findById(red.getIdVrsteKriterija())
                    .orElseThrow(() -> new PrijavaException("Nepoznat kriterij."));

            if (v.isTraziParametar() && red.getParametar() == null) {
                throw new PrijavaException(
                        "Za kriterij '" + v.getNaziv() + "' morate unijeti "
                                + v.getNazivParametra().toLowerCase() + ".");
            }

            PrijavaKriterij k = new PrijavaKriterij();
            k.setVrsta(v);
            k.setParametar(v.isTraziParametar() ? red.getParametar() : null);
            k.setVerifikovan(false);   // uvijek ceka administratora
            p.dodajKriterij(k);
        }
    }

    // ---------- PODNOSENJE ----------

    @Transactional
    public void podnesi(Integer idPrijave, Integer idStudenta) {

        Prijava p = dohvatiZaIzmjenu(idPrijave, idStudenta);

        List<String> nedostaje = provjeriPotpunost(p);
        if (!nedostaje.isEmpty()) {
            throw new PrijavaException(
                    "Prijava nije potpuna: " + String.join(", ", nedostaje) + ".");
        }

        p.setDatumPrijave(LocalDate.now());
        p.setStatus(status(Statusi.NA_PREGLEDU));

        // kes potvrdjenih bodova; u ovom trenutku jos nista nije verifikovano
        p.setUkupniBodovi(bodovanjeService.izracunaj(p).potvrdjeno());
    }

    private List<String> provjeriPotpunost(Prijava p) {
        List<String> nedostaje = new ArrayList<>();

        if (p.getGodinaStudija() == null) nedostaje.add("godina studija");
        if (p.getProsjek() == null) nedostaje.add("prosjek");
        if (p.getBrojClanovaDomacinstva() == null) nedostaje.add("broj članova domaćinstva");
        if (p.getUkupnaPrimanja() == null) nedostaje.add("primanja domaćinstva");
        if (p.getUdaljenostKm() == null) nedostaje.add("udaljenost do doma");

        return nedostaje;
    }

    // ---------- ODUSTAJANJE ----------

    @Transactional
    public void odustani(Integer idPrijave, Integer idStudenta) {
        Prijava p = dohvatiZaIzmjenu(idPrijave, idStudenta);
        prijavaRepository.delete(p);
    }

    // ---------- CITANJE ----------

    @Transactional(readOnly = true)
    public Prijava dohvatiSvoju(Integer idPrijave, Integer idStudenta) {
        Prijava p = prijavaRepository.findById(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));

        provjeriVlasnistvo(p, idStudenta);
        return p;
    }

    // ---------- POMOCNE ----------

    /**
     * Dohvata prijavu i provjerava da pripada studentu i da je jos u izradi.
     * Ovo je jedina tacka kroz koju prolaze sve izmjene.
     */
    private Prijava dohvatiZaIzmjenu(Integer idPrijave, Integer idStudenta) {

        Prijava p = prijavaRepository.findById(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));

        provjeriVlasnistvo(p, idStudenta);

        if (!Statusi.U_IZRADI.equals(nazivStatusa(p))) {
            throw new PrijavaException(
                    "Podnesena prijava se ne može mijenjati. "
                            + "Za izmjene se obratite administraciji doma.");
        }

        return p;
    }

    /** Sprjecava da student vidi ili mijenja tudju prijavu preko URL-a. */
    private void provjeriVlasnistvo(Prijava p, Integer idStudenta) {
        if (p.getStudent() == null
                || !p.getStudent().getIdStudent().equals(idStudenta)) {
            throw new PrijavaException("Nemate pristup ovoj prijavi.");
        }
    }

    private String nazivStatusa(Prijava p) {
        return p.getStatus() != null ? p.getStatus().getNaziv() : null;
    }

    private StatusPrijave status(String naziv) {
        return statusRepository.findByNaziv(naziv)
                .orElseThrow(() -> new IllegalStateException(
                        "Status '" + naziv + "' ne postoji u bazi."));
    }

    @Transactional(readOnly = true)
    public List<PrijavaPregledDTO> pregledZaStudenta(Integer idStudenta) {
        return prijavaRepository.nadjiZaStudenta(idStudenta).stream()
                .map(p -> new PrijavaPregledDTO(
                        p.getIdPrijava(),
                        p.getAkademskaGodina(),
                        p.getDatumPrijave(),
                        p.getStatus() != null ? p.getStatus().getNaziv() : "Nepoznat",
                        p.getUkupniBodovi(),
                        p.getDokumenti() != null ? p.getDokumenti().size() : 0
                ))
                .toList();
    }
}