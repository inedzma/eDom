package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.MojSmjestaj;
import ba.unze.edom.server.dto.ZahtjevForma;
import ba.unze.edom.server.dto.ZeljaForma;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stranica "Moj smještaj" za studenta.
 * Prije dodjele sobe: želje. Poslije dodjele: soba, cimeri i zahtjev za promjenu.
 * Ništa se ne mijenja samo od sebe: sve pregleda i rješava administrator.
 */
@Service
@RequiredArgsConstructor
public class StudentSmjestajService {

    private final SmjestajRepository smjestajRepository;
    private final ZeljaSmjestajaRepository zeljaRepository;
    private final ZahtjevPromjeneSobeRepository zahtjevRepository;
    private final SobaRepository sobaRepository;

    // ---------- prikaz ----------

    @Transactional(readOnly = true)
    public Optional<MojSmjestaj> stanje(Integer idStudenta) {
        Optional<Prijava> op = posljednjaPrijava(idStudenta);
        if (op.isEmpty()) return Optional.empty();
        Prijava p = op.get();

        MojSmjestaj.Soba soba = null;
        Optional<Smjestaj> sm = dodijeljenSmjestaj(p);
        if (sm.isPresent()) {
            Soba s = sm.get().getSoba();
            List<MojSmjestaj.Cimer> cimeri = smjestajRepository.stanariSobe(s.getIdSoba(), p.getAkademskaGodina()).stream()
                    .filter(x -> !x.getPrijava().getIdPrijava().equals(p.getIdPrijava()))
                    .filter(x -> !Smjestaj.PREDLOZEN.equals(x.getStatus()))
                    .map(x -> {
                        Student c = x.getPrijava().getStudent();
                        return new MojSmjestaj.Cimer(c.getIme() + " " + c.getPrezime(), c.getTelefon(), c.getEmail());
                    })
                    .toList();
            soba = new MojSmjestaj.Soba(s.getBroj(), s.getSprat().getNaziv(), s.getKapacitet(),
                    Smjestaj.USELJEN.equals(sm.get().getStatus()), sm.get().getDatumUseljenja(), cimeri);
        }

        List<MojSmjestaj.Zahtjev> zahtjevi = zahtjevRepository
                .findByPrijava_IdPrijavaOrderByDatumPodnosenjaDesc(p.getIdPrijava()).stream()
                .map(z -> new MojSmjestaj.Zahtjev(z.getIdZahtjev(), z.getVrsta(), opis(z), z.getRazlog(),
                        z.getStatus(), z.getDatumPodnosenja(), z.getOdgovor()))
                .toList();

        return Optional.of(new MojSmjestaj(
                p.getAkademskaGodina(),
                p.getStatus() != null ? p.getStatus().getNaziv() : null,
                primljen(p), soba,
                zeljaRepository.findByPrijava_IdPrijava(p.getIdPrijava()).isPresent(),
                zahtjevi,
                zahtjevi.stream().anyMatch(MojSmjestaj.Zahtjev::naCekanju)));
    }

    /** Sačuvana želja, za popunjavanje forme. */
    @Transactional(readOnly = true)
    public ZeljaForma zelja(Integer idStudenta) {
        ZeljaForma f = new ZeljaForma();
        posljednjaPrijava(idStudenta)
                .flatMap(p -> zeljaRepository.findByPrijava_IdPrijava(p.getIdPrijava()))
                .ifPresent(z -> {
                    f.setZeljeniKapacitet(z.getZeljeniKapacitet());
                    f.setZeljenaSoba(z.getSoba() != null ? z.getSoba().getBroj() : null);
                    f.setCimer1(z.getCimer1());
                    f.setCimer2(z.getCimer2());
                    f.setNapomena(z.getNapomena());
                });
        return f;
    }

    // ---------- želje (samo dok soba nije dodijeljena) ----------

    @Transactional
    public void sacuvajZelju(Integer idStudenta, ZeljaForma f) {
        Prijava p = posljednjaPrijava(idStudenta)
                .orElseThrow(() -> new PrijavaException("Nemate prijavu."));
        if (!primljen(p)) throw new PrijavaException("Želje se mogu unijeti tek kada je prijava odobrena.");
        if (dodijeljenSmjestaj(p).isPresent()) {
            throw new PrijavaException("Soba vam je već dodijeljena. Za promjenu podnesite zahtjev za promjenu sobe.");
        }

        Integer kapacitet = f.getZeljeniKapacitet();
        if (kapacitet != null && kapacitet != 2 && kapacitet != 3) kapacitet = null;

        Soba soba = null;
        if (f.getZeljenaSoba() != null) {
            soba = sobaRepository.findByBroj(f.getZeljenaSoba())
                    .filter(Soba::zaRaspored)
                    .orElseThrow(() -> new PrijavaException("Soba " + f.getZeljenaSoba() + " ne postoji ili nije za studente."));
        }

        ZeljaSmjestaja z = zeljaRepository.findByPrijava_IdPrijava(p.getIdPrijava()).orElseGet(ZeljaSmjestaja::new);
        z.setPrijava(p);
        z.setZeljeniKapacitet(kapacitet);
        z.setSoba(soba);
        z.setCimer1(prazno(f.getCimer1()));
        z.setCimer2(prazno(f.getCimer2()));
        z.setNapomena(prazno(f.getNapomena()));
        zeljaRepository.save(z);
    }

    // ---------- zahtjev za promjenu sobe (samo kad je soba dodijeljena) ----------

    @Transactional
    public void podnesiZahtjev(Integer idStudenta, ZahtjevForma f) {
        Prijava p = posljednjaPrijava(idStudenta)
                .orElseThrow(() -> new PrijavaException("Nemate prijavu."));
        if (dodijeljenSmjestaj(p).isEmpty()) {
            throw new PrijavaException("Zahtjev za promjenu se podnosi tek kada vam je soba dodijeljena.");
        }
        if (zahtjevRepository.existsByPrijava_IdPrijavaAndStatus(p.getIdPrijava(), ZahtjevPromjeneSobe.NA_CEKANJU)) {
            throw new PrijavaException("Već imate zahtjev na čekanju. Povucite ga ako želite podnijeti novi.");
        }

        String vrsta = ZahtjevPromjeneSobe.PREMJESTAJ.equals(f.getVrsta())
                ? ZahtjevPromjeneSobe.PREMJESTAJ : ZahtjevPromjeneSobe.ZAMJENA;
        String sa = prazno(f.getSaStudentom());
        String razlog = prazno(f.getRazlog());

        if (ZahtjevPromjeneSobe.ZAMJENA.equals(vrsta) && sa == null) {
            throw new PrijavaException("Upišite broj indeksa ili email studenta s kojim mijenjate sobu.");
        }
        if (razlog == null) throw new PrijavaException("Upišite razlog zahtjeva.");

        ZahtjevPromjeneSobe z = new ZahtjevPromjeneSobe();
        z.setPrijava(p);
        z.setVrsta(vrsta);
        z.setSaStudentom(ZahtjevPromjeneSobe.ZAMJENA.equals(vrsta) ? sa : null);
        z.setZeljenaSoba(ZahtjevPromjeneSobe.PREMJESTAJ.equals(vrsta) ? f.getZeljenaSoba() : null);
        z.setRazlog(razlog);
        z.setStatus(ZahtjevPromjeneSobe.NA_CEKANJU);
        z.setDatumPodnosenja(LocalDateTime.now());
        zahtjevRepository.save(z);
    }

    /** Student povlači svoj zahtjev dok je na čekanju. */
    @Transactional
    public void povuciZahtjev(Integer idStudenta, Integer idZahtjeva) {
        ZahtjevPromjeneSobe z = zahtjevRepository.findById(idZahtjeva)
                .orElseThrow(() -> new PrijavaException("Zahtjev ne postoji."));
        if (!z.getPrijava().getStudent().getIdStudent().equals(idStudenta)) {
            throw new PrijavaException("Zahtjev ne postoji.");
        }
        if (!ZahtjevPromjeneSobe.NA_CEKANJU.equals(z.getStatus())) {
            throw new PrijavaException("Zahtjev je već riješen.");
        }
        z.setStatus(ZahtjevPromjeneSobe.POVUCEN);
        z.setDatumOdgovora(LocalDateTime.now());
    }

    // ---------- pomoćne ----------

    private Optional<Prijava> posljednjaPrijava(Integer idStudenta) {
        return smjestajRepository.prijaveStudenta(idStudenta).stream().findFirst();
    }

    private static boolean primljen(Prijava p) {
        return p.getStatus() != null && SobeService.STATUSI_ZA_RASPORED.contains(p.getStatus().getNaziv());
    }

    /** Dodijeljena ili useljena soba; nepotvrđen prijedlog se ne računa. */
    private Optional<Smjestaj> dodijeljenSmjestaj(Prijava p) {
        return smjestajRepository.aktivniZaPrijavu(p.getIdPrijava())
                .filter(sm -> !Smjestaj.PREDLOZEN.equals(sm.getStatus()));
    }

    private static String opis(ZahtjevPromjeneSobe z) {
        if (ZahtjevPromjeneSobe.ZAMJENA.equals(z.getVrsta())) {
            return "Zamjena sobe sa studentom " + z.getSaStudentom();
        }
        return "Premještaj" + (z.getZeljenaSoba() != null ? " u sobu " + z.getZeljenaSoba() : " u drugu sobu");
    }

    private static String prazno(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}