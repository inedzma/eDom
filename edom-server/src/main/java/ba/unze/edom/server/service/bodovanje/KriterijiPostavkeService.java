package ba.unze.edom.server.service.bodovanje;

import ba.unze.edom.common.dto.bodovanje.KriterijPostavkaDTO;
import ba.unze.edom.common.dto.bodovanje.KriterijiPostavkeDTO;
import ba.unze.edom.server.entity.VrstaDokumenta;
import ba.unze.edom.server.entity.VrstaKriterija;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.VrstaDokumentaRepository;
import ba.unze.edom.server.repository.VrstaKriterijaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;

/** Ekran Postavke → Kriteriji bodovanja (tabela vrsta_kriterija). */
@Service
@RequiredArgsConstructor
public class KriterijiPostavkeService {

    private static final Set<String> GRUPE = Set.of(GrupeKriterija.BRANIOCI, GrupeKriterija.DODATNI);
    /** Bodove ovih kriterija računa BodovanjeService iz parametra/perioda. */
    private static final Set<String> RACUNA_SE = Set.of("STUDENT_RVI", "INVALID_RODITELJA", "DIJETE_OSRBIH");

    @PersistenceContext
    private EntityManager em;

    private final VrstaKriterijaRepository vrstaKriterijaRepository;
    private final VrstaDokumentaRepository vrstaDokumentaRepository;

    @Transactional(readOnly = true)
    public KriterijiPostavkeDTO lista() {
        Map<Integer, Long> broj = brojPrijava();

        List<KriterijPostavkaDTO> kriteriji = vrstaKriterijaRepository.findAll().stream()
                .sorted(Comparator.comparing((VrstaKriterija v) -> !v.isAktivan())
                        .thenComparing(v -> v.getGrupa() != null ? v.getGrupa() : "")
                        .thenComparing(v -> v.getNaziv() != null ? v.getNaziv() : ""))
                .map(v -> dto(v, broj.getOrDefault(v.getIdVrstaKriterija(), 0L)))
                .toList();

        List<KriterijiPostavkeDTO.Dokument> dokumenti = vrstaDokumentaRepository.findAll().stream()
                .sorted(Comparator.comparing(d -> d.getNaziv() != null ? d.getNaziv() : ""))
                .map(d -> new KriterijiPostavkeDTO.Dokument(d.getIdVrsta(), d.getNaziv()))
                .toList();

        return new KriterijiPostavkeDTO(kriteriji, dokumenti);
    }

    /** Novi (id == null) ili izmjena postojećeg. */
    @Transactional
    public KriterijiPostavkeDTO sacuvaj(KriterijPostavkaDTO z) {
        String naziv = ocisti(z.naziv());
        if (naziv == null) throw new PrijavaException("Upišite naziv kriterija.");
        if (!GRUPE.contains(z.grupa())) throw new PrijavaException("Odaberite grupu kriterija.");

        VrstaKriterija v;
        if (z.id() == null) {
            v = new VrstaKriterija();
            v.setSifra(novaSifra(naziv));
        } else {
            v = vrstaKriterijaRepository.findById(z.id())
                    .orElseThrow(() -> new PrijavaException("Kriterij ne postoji."));
        }

        boolean racunaSe = RACUNA_SE.contains(v.getSifra());
        if (!racunaSe) {
            if (z.bodovi() == null) throw new PrijavaException("Upišite koliko bodova kriterij nosi.");
            if (z.bodovi().signum() < 0) throw new PrijavaException("Bodovi ne mogu biti negativni.");
        }
        String nazivParametra = ocisti(z.nazivParametra());
        if (z.traziParametar() && nazivParametra == null) {
            throw new PrijavaException("Upišite šta se unosi kao parametar (npr. \"Broj priznanja\").");
        }

        v.setNaziv(naziv);
        v.setGrupa(z.grupa());
        v.setBodovi(racunaSe ? null : z.bodovi());     // formula ostaje u kodu
        v.setTraziParametar(z.traziParametar());
        v.setNazivParametra(z.traziParametar() ? nazivParametra : null);
        v.setTraziPeriod(z.traziPeriod());
        v.setAktivan(z.aktivan());
        v.setDokaz(z.idDokaz() == null ? null : vrstaDokumentaRepository.findById(z.idDokaz())
                .orElseThrow(() -> new PrijavaException("Nepoznata vrsta dokumenta.")));

        vrstaKriterijaRepository.save(v);
        return lista();
    }

    /** Brisanje samo ako ga nijedna prijava ne koristi; inače se deaktivira. */
    @Transactional
    public KriterijiPostavkeDTO obrisi(Integer id) {
        VrstaKriterija v = vrstaKriterijaRepository.findById(id)
                .orElseThrow(() -> new PrijavaException("Kriterij ne postoji."));
        if (RACUNA_SE.contains(v.getSifra())) {
            throw new PrijavaException("Ovaj kriterij se računa formulom i ne može se obrisati. Možete ga deaktivirati.");
        }
        long n = brojPrijava().getOrDefault(id, 0L);
        if (n > 0) {
            throw new PrijavaException("Kriterij koristi " + n + " prijava, pa se ne može obrisati. Deaktivirajte ga.");
        }
        vrstaKriterijaRepository.delete(v);
        return lista();
    }

    // ---------- pomoćne ----------

    private KriterijPostavkaDTO dto(VrstaKriterija v, long brojPrijava) {
        VrstaDokumenta d = v.getDokaz();
        return new KriterijPostavkaDTO(
                v.getIdVrstaKriterija(), v.getSifra(), v.getNaziv(), v.getBodovi(), v.getGrupa(),
                v.isTraziParametar(), v.getNazivParametra(), v.isTraziPeriod(), v.isAktivan(),
                d != null ? d.getIdVrsta() : null, d != null ? d.getNaziv() : null,
                brojPrijava, RACUNA_SE.contains(v.getSifra()));
    }

    private Map<Integer, Long> brojPrijava() {
        Map<Integer, Long> m = new HashMap<>();
        for (Object[] r : em.createQuery(
                "SELECT k.vrsta.idVrstaKriterija, COUNT(k) FROM PrijavaKriterij k GROUP BY k.vrsta.idVrstaKriterija",
                Object[].class).getResultList()) {
            m.put((Integer) r[0], (Long) r[1]);
        }
        return m;
    }

    /** "Dijete nosioca ratnih priznanja" → DIJETE_NOSIOCA_RATNIH_PRIZNANJA (jedinstveno). */
    private String novaSifra(String naziv) {
        String s = naziv.replace("đ", "dj").replace("Đ", "DJ");
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (s.isEmpty()) s = "KRITERIJ";
        if (s.length() > 30) s = s.substring(0, 30).replaceAll("_+$", "");

        String sifra = s;
        int i = 2;
        while (vrstaKriterijaRepository.findBySifra(sifra).isPresent()) {
            sifra = s + "_" + i++;
        }
        return sifra;
    }

    private static String ocisti(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}