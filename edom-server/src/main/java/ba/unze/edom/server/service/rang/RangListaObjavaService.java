package ba.unze.edom.server.service.rang;

import ba.unze.edom.common.dto.rang.ObjavaRangListeDTO;
import ba.unze.edom.common.dto.rang.RangListaAdminDTO;
import ba.unze.edom.common.dto.rang.RangObjavaDTO;
import ba.unze.edom.common.dto.rang.RangRedDTO;
import ba.unze.edom.server.dto.ObjavljenaRangLista;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Objava rang liste bez tabela: lista se snimi u jedan fajl,
 * web prikazuje taj fajl dok admin ne objavi ponovo.
 */
@Service
public class RangListaObjavaService {

    private static final Logger log = LoggerFactory.getLogger(RangListaObjavaService.class);
    private static final Set<String> VRSTE = Set.of("PRELIMINARNA", "KONACNA");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");

    private final RangListaService rangListaService;
    private final Path fajl;

    private ObjavljenaRangLista zadnja;
    private boolean ucitano;

    public RangListaObjavaService(RangListaService rangListaService,
                                  @Value("${edom.rang.objava-fajl:data/rang-lista-objava.ser}") String putanja) {
        this.rangListaService = rangListaService;
        this.fajl = Path.of(putanja);
    }

    /** "Objavi na webu": trenutna lista → fajl. Prethodna objava se prepisuje. */
    public synchronized RangObjavaDTO objavi(ObjavaRangListeDTO z) {
        if (z == null || z.vrsta() == null || !VRSTE.contains(z.vrsta())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vrsta liste mora biti PRELIMINARNA ili KONACNA.");
        }

        if ((z.useljenjeOd() == null) != (z.useljenjeDo() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unesite oba datuma useljenja ili nijedan.");
        }
        if (z.useljenjeOd() != null && z.useljenjeOd().isAfter(z.useljenjeDo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Početak useljenja je poslije kraja useljenja.");
        }

        RangListaAdminDTO l = rangListaService.zaAdmina(null, null, z.popuniBrucosima());
        if (l.godina() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nema prijava za objavu.");
        }

        ObjavljenaRangLista o = new ObjavljenaRangLista(
                z.vrsta(),
                LocalDateTime.now(ZONA).format(FMT),
                trenutniAdmin(),
                l.akademskaGodina(),
                l.kapacitet(),
                l.slobodno(),
                stavke(l.brucosi()),
                stavke(l.saUslovom()),
                stavke(l.listaCekanja()),
                z.useljenjeOd(),
                z.useljenjeDo());

        if (o.brojStudenata() == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Na listi nema nijedne ispravne prijave za objavu.");
        }

        snimi(o);
        zadnja = o;
        ucitano = true;
        return dto(o);
    }

    /** Šta je trenutno na webu; null ako ništa nije objavljeno. */
    public synchronized ObjavljenaRangLista zadnja() {
        if (!ucitano) {
            zadnja = procitaj();
            ucitano = true;
        }
        return zadnja;
    }

    /** Za desktop traku "Na webu: …". */
    public RangObjavaDTO zadnjaInfo() {
        ObjavljenaRangLista o = zadnja();
        return o == null ? RangObjavaDTO.nema() : dto(o);
    }

    // ---------- pomoćne ----------

    /** Neispravne prijave se ne objavljuju; one su na dnu, pa se redni brojevi ne kvare. */
    private List<ObjavljenaRangLista.Stavka> stavke(List<RangRedDTO> redovi) {
        List<ObjavljenaRangLista.Stavka> out = new ArrayList<>();
        int rb = 0;
        for (RangRedDTO r : redovi) {
            if (r.greska() != null) continue;
            out.add(new ObjavljenaRangLista.Stavka(++rb, r.idStudent(), r.sifra(), r.ukupno(), r.primljen()));
        }
        return out;
    }

    private void snimi(ObjavljenaRangLista o) {
        try {
            if (fajl.getParent() != null) Files.createDirectories(fajl.getParent());
            Path privremeni = fajl.resolveSibling(fajl.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(privremeni))) {
                out.writeObject(o);
            }
            // zamjena tek kad je novi fajl potpun — web nikad ne vidi pola liste
            Files.move(privremeni, fajl, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("Objava rang liste nije snimljena u {}", fajl.toAbsolutePath(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Rang lista nije objavljena: greška pri snimanju na serveru.");
        }
    }

    private ObjavljenaRangLista procitaj() {
        if (!Files.exists(fajl)) return null;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(fajl))) {
            return (ObjavljenaRangLista) in.readObject();
        } catch (Exception e) {
            // npr. struktura se promijenila u kodu — admin samo ponovo objavi
            log.warn("Objavljena rang lista se ne može pročitati ({}). Objavite je ponovo.", e.getMessage());
            return null;
        }
    }

    private RangObjavaDTO dto(ObjavljenaRangLista o) {
        Integer godina = null;
        try {
            godina = Integer.valueOf(o.akademskaGodina().substring(0, 4));
        } catch (Exception ignored) { }
        return new RangObjavaDTO(1, godina, o.vrsta(), o.objavljeno(), o.objavio(), o.brojStudenata());
    }

    private String trenutniAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : "admin";
    }
}