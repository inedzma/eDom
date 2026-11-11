package ba.unze.edom.server.service.sobe;

import ba.unze.edom.server.dto.MojSmjestaj;
import ba.unze.edom.server.dto.ZeljaForma;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.SmjestajRepository;
import ba.unze.edom.server.repository.SobaRepository;
import ba.unze.edom.server.repository.ZeljaSmjestajaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Stranica "Moj smještaj" za studenta.
 * Želja se može mijenjati u toku cijele godine: prije dodjele služi za raspored,
 * poslije dodjele kao zahtjev za promjenu sobe (razlog u napomeni).
 * Ništa se ne mijenja samo od sebe: administrator odlučuje i odgovara e-mailom.
 */
@Service
@RequiredArgsConstructor
public class StudentSmjestajService {

    private final SmjestajRepository smjestajRepository;
    private final ZeljaSmjestajaRepository zeljaRepository;
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

        return Optional.of(new MojSmjestaj(
                p.getAkademskaGodina(),
                p.getStatus() != null ? p.getStatus().getNaziv() : null,
                primljen(p), soba,
                zeljaRepository.findByPrijava_IdPrijava(p.getIdPrijava()).isPresent()));
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

    // ---------- želja ----------

    @Transactional
    public void sacuvajZelju(Integer idStudenta, ZeljaForma f) {
        Prijava p = posljednjaPrijava(idStudenta)
                .orElseThrow(() -> new PrijavaException("Nemate prijavu."));
        if (!primljen(p)) throw new PrijavaException("Želje se mogu unijeti tek kada je prijava odobrena.");

        boolean imaSobu = dodijeljenSmjestaj(p).isPresent();
        String napomena = prazno(f.getNapomena());
        if (imaSobu && napomena == null) {
            throw new PrijavaException("Soba vam je već dodijeljena. U napomenu upišite razlog promjene sobe.");
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
        z.setNapomena(napomena);
        z.setDatumIzmjene(LocalDateTime.now());
        zeljaRepository.save(z);
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

    private static String prazno(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}