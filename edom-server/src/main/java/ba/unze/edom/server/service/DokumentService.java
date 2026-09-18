package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.DokumentPregled;
import ba.unze.edom.server.entity.Dokument;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.VrstaDokumenta;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.DokumentRepository;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.VrstaDokumentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DokumentService {

    private final DokumentRepository dokumentRepository;
    private final PrijavaRepository prijavaRepository;
    private final VrstaDokumentaRepository vrstaDokumentaRepository;

    @Value("${edom.dokumenti.max-velicina-mb:2}")
    private int maxVelicinaMb;

    @Value("${edom.dokumenti.max-sirina-px:1600}")
    private int maxSirinaPx;

    private static final Set<String> DOZVOLJENI_TIPOVI =
            Set.of("image/jpeg", "image/png", "application/pdf");

    private static final Set<String> SLIKE =
            Set.of("image/jpeg", "image/png");

    /** Statusi u kojima se dokumenti jos smiju dodavati ili brisati. */
    private static final Set<String> OTVORENI_STATUSI =
            Set.of(Statusi.U_IZRADI, Statusi.NA_PREGLEDU);

    // ---------- CITANJE ----------

    @Transactional(readOnly = true)
    public List<DokumentPregled> zaPrijavu(Integer idPrijave) {
        return dokumentRepository
                .findByPrijava_IdPrijavaOrderByIdDokumentAsc(idPrijave);
    }

    @Transactional(readOnly = true)
    public List<VrstaDokumenta> sveVrste() {
        return vrstaDokumentaRepository.findAll(Sort.by("naziv"));
    }

    /** Vraca Base64 sadrzaj. Poziva se samo pri preuzimanju. */
    @Transactional(readOnly = true)
    public String sadrzaj(Integer idDokumenta, Integer idStudenta) {

        Dokument d = dokumentRepository.findById(idDokumenta)
                .orElseThrow(() -> new PrijavaException("Dokument ne postoji."));

        provjeriVlasnistvo(d.getPrijava(), idStudenta);

        return dokumentRepository.nadjiSadrzaj(idDokumenta)
                .orElseThrow(() -> new PrijavaException(
                        "Dokument nema prilozenu datoteku."));
    }

    // ---------- UPLOAD ----------

    @Transactional
    public void dodaj(Integer idPrijave, Integer idStudenta,
                      Integer idVrste, MultipartFile fajl, String naziv) {

        Prijava p = dohvatiOtvorenu(idPrijave, idStudenta);

        if (fajl == null || fajl.isEmpty()) {
            throw new PrijavaException("Niste odabrali datoteku.");
        }

        String tip = fajl.getContentType();
        if (tip == null || !DOZVOLJENI_TIPOVI.contains(tip)) {
            throw new PrijavaException(
                    "Dozvoljeni su samo JPG, PNG i PDF dokumenti.");
        }

        VrstaDokumenta vrsta = vrstaDokumentaRepository.findById(idVrste)
                .orElseThrow(() -> new PrijavaException("Nepoznata vrsta dokumenta."));

        byte[] podaci = pripremi(fajl, tip);

        long maxBajtova = (long) maxVelicinaMb * 1024 * 1024;
        if (podaci.length > maxBajtova) {
            throw new PrijavaException(
                    "Datoteka je prevelika. Najveca dozvoljena velicina je "
                            + maxVelicinaMb + " MB.");
        }

        Dokument d = new Dokument();
        d.setNaziv(naziv != null && !naziv.isBlank()
                ? naziv.trim() : vrsta.getNaziv());
        d.setVrstaDokumenta(vrsta);
        d.setDatumUpload(LocalDate.now());
        d.setDostavljen(true);
        d.setBrojBodova(0);   // bodovi se vise ne drze na dokumentu
        d.setDokumentB64(Base64.getEncoder().encodeToString(podaci));
        d.setPrijava(p);

        dokumentRepository.save(d);
    }

    // ---------- BRISANJE ----------

    @Transactional
    public void obrisi(Integer idDokumenta, Integer idStudenta) {

        Dokument d = dokumentRepository.findById(idDokumenta)
                .orElseThrow(() -> new PrijavaException("Dokument ne postoji."));

        Prijava p = d.getPrijava();
        provjeriVlasnistvo(p, idStudenta);

        if (!OTVORENI_STATUSI.contains(nazivStatusa(p))) {
            throw new PrijavaException(
                    "Prijava je obradjena i dokumenti se vise ne mogu mijenjati.");
        }

        dokumentRepository.delete(d);
    }

    // ---------- KOMPRESIJA ----------

    /**
     * Slike se smanjuju na najvise maxSirinaPx i ponovo kodiraju kao JPEG.
     * Skenirani dokument od 4000px je citljiv i na 1600px, a fajl je nekoliko
     * puta manji — sto je bitno jer sve ide u bazu kao Base64.
     * PDF se ne dira.
     */
    private byte[] pripremi(MultipartFile fajl, String tip) {
        try {
            if (!SLIKE.contains(tip)) {
                return fajl.getBytes();
            }

            BufferedImage original = ImageIO.read(fajl.getInputStream());
            if (original == null) {
                return fajl.getBytes();   // nije prepoznata slika
            }

            if (original.getWidth() <= maxSirinaPx) {
                return fajl.getBytes();
            }

            int novaSirina = maxSirinaPx;
            int novaVisina = (int) Math.round(
                    original.getHeight() * (novaSirina / (double) original.getWidth()));

            BufferedImage smanjena = new BufferedImage(
                    novaSirina, novaVisina, BufferedImage.TYPE_INT_RGB);

            Graphics2D g = smanjena.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, novaSirina, novaVisina, Color.WHITE, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(smanjena, "jpg", out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new PrijavaException("Datoteku nije moguce procitati.");
        }
    }

    // ---------- POMOCNE ----------

    /** Prijava u koju se jos smiju dodavati dokumenti. */
    private Prijava dohvatiOtvorenu(Integer idPrijave, Integer idStudenta) {

        Prijava p = prijavaRepository.findById(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));

        provjeriVlasnistvo(p, idStudenta);

        if (!OTVORENI_STATUSI.contains(nazivStatusa(p))) {
            throw new PrijavaException(
                    "Prijava je obradjena i dokumenti se vise ne mogu dodavati.");
        }

        return p;
    }

    /** Sprjecava da student vidi ili brise tudji dokument preko URL-a. */
    private void provjeriVlasnistvo(Prijava p, Integer idStudenta) {
        if (p == null || p.getStudent() == null
                || !p.getStudent().getIdStudent().equals(idStudenta)) {
            throw new PrijavaException("Nemate pristup ovom dokumentu.");
        }
    }

    private String nazivStatusa(Prijava p) {
        return p.getStatus() != null ? p.getStatus().getNaziv() : null;
    }
}