package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.GenerisaniDokumentDTO;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Smjestaj;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.PrijavaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Ugovor i revers iz Word obrazaca (.docx) s oznakama {{OZNAKA}}.
 * Obrazac je fajl na serveru — kad se tekst promijeni, zamijeni se samo fajl.
 */
@Service
public class ObrasciService {

    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private static final String[] RIMSKI = {"", "I", "II", "III", "IV", "V", "VI", "VII"};
    private static final Map<String, Pattern> OZNAKE = new ConcurrentHashMap<>();

    @PersistenceContext
    private EntityManager em;

    private final PrijavaRepository prijavaRepository;
    private final Path ugovorObrazac;
    private final Path reversObrazac;
    private final String cijena;
    private final String direktor;
    private final String periodOd;
    private final String periodDo;

    public ObrasciService(PrijavaRepository prijavaRepository,
                          @Value("${edom.obrasci.ugovor:data/obrasci/ugovor.docx}") String ugovor,
                          @Value("${edom.obrasci.revers:data/obrasci/revers.docx}") String revers,
                          @Value("${edom.ugovor.cijena:}") String cijena,
                          @Value("${edom.ugovor.direktor:}") String direktor,
                          @Value("${edom.ugovor.period-od:01.10.}") String periodOd,
                          @Value("${edom.ugovor.period-do:15.07.}") String periodDo) {
        this.prijavaRepository = prijavaRepository;
        this.ugovorObrazac = Path.of(ugovor);
        this.reversObrazac = Path.of(revers);
        this.cijena = cijena;
        this.direktor = direktor;
        this.periodOd = periodOd;
        this.periodDo = periodDo;
    }

    @Transactional(readOnly = true)
    public GenerisaniDokumentDTO ugovor(Integer idPrijave) {
        Map<String, String> v = podaci(idPrijave);
        return generisi(ugovorObrazac, v, "Ugovor_" + v.get("_FAJL") + ".docx");
    }

    @Transactional(readOnly = true)
    public GenerisaniDokumentDTO revers(Integer idPrijave) {
        Map<String, String> v = podaci(idPrijave);
        return generisi(reversObrazac, v, "Revers_" + v.get("_FAJL") + ".docx");
    }

    // ---------- podaci ----------

    private Map<String, String> podaci(Integer idPrijave) {
        Prijava p = prijavaRepository.findById(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));
        Student s = p.getStudent();
        Smjestaj sm = aktivniSmjestaj(idPrijave);
        if (sm == null) {
            throw new PrijavaException("Student nema dodijeljenu sobu — ugovor i revers prave se nakon dodjele sobe.");
        }

        int g = p.getAkademskaGodina();
        Integer godinaStudija = UslovUpisa.godinaStudija(p);
        LocalDate danas = LocalDate.now(ZONA);

        Map<String, String> v = new LinkedHashMap<>();
        v.put("STUDENT", n(s.getPrezime()) + (s.getImeRoditelja() != null && !s.getImeRoditelja().isBlank()
                ? " (" + s.getImeRoditelja() + ") " : " ") + n(s.getIme()));
        v.put("IME_PREZIME", n(s.getIme()) + " " + n(s.getPrezime()));
        v.put("FAKULTET", n(s.getFakultet()));
        v.put("GODINA_STUDIJA", godinaStudija != null && godinaStudija > 0 && godinaStudija < RIMSKI.length
                ? RIMSKI[godinaStudija] : (godinaStudija != null ? godinaStudija.toString() : ""));
        v.put("ADRESA", n(s.getAdresa()));
        v.put("OPCINA", n(s.getOpcina()));
        v.put("LICNA_KARTA", n(s.getBrojLicneKarte()));
        v.put("JMBG", n(s.getJmbg()));
        v.put("SOBA", sm.getSoba() != null ? String.valueOf(sm.getSoba().getBroj()) : "");
        v.put("PERIOD_OD", periodOd + g + ".");
        v.put("PERIOD_DO", periodDo + (g + 1) + ".");
        v.put("CIJENA", cijena);
        v.put("DIREKTOR", direktor);
        v.put("GODINA", String.valueOf(danas.getYear()));
        v.put("DANAS", danas.format(DATUM));
        v.put("AKADEMSKA_GODINA", g + "/" + (g + 1));
        v.put("SIFRA", SifraStudenta.format(s.getSifraBroj(), g));
        v.put("_FAJL", (n(s.getPrezime()) + "_" + n(s.getIme())).replaceAll("[\\\\/:*?\"<>|\\s]+", "_"));
        return v;
    }

    /** Zadnji smještaj koji nije ISELJEN; ako su svi iseljeni, zadnji. */
    private Smjestaj aktivniSmjestaj(Integer idPrijave) {
        List<Smjestaj> svi = em.createQuery(
                        "SELECT s FROM Smjestaj s LEFT JOIN FETCH s.soba WHERE s.prijava.idPrijava = :id "
                                + "ORDER BY s.idSmjestaj DESC", Smjestaj.class)
                .setParameter("id", idPrijave)
                .getResultList();
        return svi.stream()
                .filter(x -> !Smjestaj.ISELJEN.equals(x.getStatus()))
                .findFirst()
                .orElse(svi.isEmpty() ? null : svi.get(0));
    }

    // ---------- docx ----------

    private GenerisaniDokumentDTO generisi(Path obrazac, Map<String, String> v, String naziv) {
        if (!Files.exists(obrazac)) {
            throw new PrijavaException("Nema obrasca " + obrazac.toAbsolutePath()
                    + ". Snimite obrazac kao .docx na to mjesto.");
        }
        try {
            byte[] ulaz = Files.readAllBytes(obrazac);
            ByteArrayOutputStream izlaz = new ByteArrayOutputStream();

            try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(ulaz));
                 ZipOutputStream zout = new ZipOutputStream(izlaz)) {
                ZipEntry e;
                while ((e = zin.getNextEntry()) != null) {
                    byte[] b = zin.readAllBytes();
                    String ime = e.getName();
                    boolean tekst = ime.equals("word/document.xml")
                            || (ime.startsWith("word/header") && ime.endsWith(".xml"))
                            || (ime.startsWith("word/footer") && ime.endsWith(".xml"));
                    if (tekst) {
                        b = popuni(new String(b, StandardCharsets.UTF_8), v).getBytes(StandardCharsets.UTF_8);
                    }
                    zout.putNextEntry(new ZipEntry(ime));
                    zout.write(b);
                    zout.closeEntry();
                }
            }
            return new GenerisaniDokumentDTO(naziv, Base64.getEncoder().encodeToString(izlaz.toByteArray()));
        } catch (IOException e) {
            throw new PrijavaException("Obrazac " + obrazac.getFileName() + " se ne može pročitati.");
        }
    }

    /**
     * Zamjena {{OZNAKA}}. Word često razbije tekst na više dijelova,
     * pa uzorak dozvoljava XML oznake između slova i uklanja ih pri zamjeni.
     */
    private String popuni(String xml, Map<String, String> v) {
        for (Map.Entry<String, String> e : v.entrySet()) {
            if (e.getKey().startsWith("_")) continue;
            Pattern p = OZNAKE.computeIfAbsent(e.getKey(), ObrasciService::uzorak);
            xml = p.matcher(xml).replaceAll(Matcher.quoteReplacement(esc(e.getValue())));
        }
        return xml;
    }

    private static Pattern uzorak(String kljuc) {
        String oznaka = "{{" + kljuc + "}}";
        StringJoiner j = new StringJoiner("(?:<[^>]+>)*");
        for (char c : oznaka.toCharArray()) j.add(Pattern.quote(String.valueOf(c)));
        return Pattern.compile(j.toString());
    }

    private static String esc(String t) {
        if (t == null) return "";
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }
}