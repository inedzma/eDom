package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.sobe.*;
import ba.unze.edom.common.dto.student.NovaNapomenaDTO;
import ba.unze.edom.common.dto.student.StudentDetaljiDTO;
import ba.unze.edom.server.service.sobe.SobeService;
import ba.unze.edom.server.service.student.StudentDetaljiService;
import ba.unze.edom.server.service.sobe.ZahtjeviSobaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Sobe i raspored (samo desktop / administratori). */
@RestController
@RequestMapping("/api/sobe")
@RequiredArgsConstructor
public class SobeApiController {

    private static final Map<String, Object> OK = Map.of("ok", true);

    private final SobeService sobeService;

    private final ZahtjeviSobaService zahtjeviService;

    private final StudentDetaljiService detaljiService;

    @GetMapping
    public SobePregledDTO pregled(@RequestParam int godina) {
        return sobeService.pregled(godina);
    }

    @GetMapping("/bez-sobe")
    public List<StudentBezSobeDTO> bezSobe(@RequestParam int godina) {
        return sobeService.bezSobe(godina);
    }

    @GetMapping("/oznake")
    public List<OznakaStanaraDTO> oznake() {
        return sobeService.oznake();
    }

    // ---------- raspored ----------

    @PostMapping("/raspored/prijedlog")
    public PrijedlogRasporedaDTO prijedlog(@RequestParam int godina) {
        return sobeService.napraviPrijedlog(godina);
    }

    @PostMapping("/raspored/potvrdi")
    public Map<String, Object> potvrdi(@RequestParam int godina) {
        return Map.of("broj", sobeService.potvrdiPrijedlog(godina));
    }

    @PostMapping("/raspored/ponisti")
    public Map<String, Object> ponisti(@RequestParam int godina) {
        return Map.of("broj", sobeService.ponistiPrijedlog(godina));
    }

    // ---------- ručne izmjene ----------

    @PostMapping("/dodijeli")
    public Map<String, Object> dodijeli(@Valid @RequestBody DodjelaSobeDTO z) {
        sobeService.dodijeli(z);
        return OK;
    }

    @PostMapping("/smjestaj/{id}/useli")
    public Map<String, Object> useli(@PathVariable Integer id, @RequestParam(defaultValue = "0") int kljucevi) {
        sobeService.useli(id, kljucevi);
        return OK;
    }

    /** Vraća {"nedostaje": broj ključeva koji nisu vraćeni}. */
    @PostMapping("/smjestaj/{id}/iseli")
    public Map<String, Object> iseli(@PathVariable Integer id, @RequestParam(defaultValue = "0") int vraceno) {
        return Map.of("nedostaje", sobeService.iseli(id, vraceno));
    }

    @PostMapping("/smjestaj/{id}/ukloni")
    public Map<String, Object> ukloni(@PathVariable Integer id) {
        sobeService.ukloni(id);
        return OK;
    }

    @PostMapping("/smjestaj/{id}/oznaka")
    public Map<String, Object> oznaka(@PathVariable Integer id, @RequestParam(required = false) Integer idOznake) {
        sobeService.postaviOznaku(id, idOznake);
        return OK;
    }

    @PostMapping("/{id}/aktivna")
    public Map<String, Object> aktivna(@PathVariable Integer id, @RequestParam boolean aktivna,
                                       @RequestParam(required = false) String napomena) {
        sobeService.postaviAktivna(id, aktivna, napomena);
        return OK;
    }

    // ---------- zahtjevi za promjenu sobe ----------

    @GetMapping("/zahtjevi")
    public List<ZahtjevSobeDTO> zahtjevi(@RequestParam int godina) {
        return zahtjeviService.zaGodinu(godina);
    }

    @PostMapping("/zahtjevi/{id}/odgovor")
    public Map<String, Object> odgovor(@PathVariable Integer id, @RequestParam String status,
                                       @RequestParam(required = false) String odgovor) {
        zahtjeviService.odgovori(id, status, odgovor);
        return OK;
    }

    // ---------- detalji studenta i napomena ----------

    @GetMapping("/student/{idPrijava}")
    public StudentDetaljiDTO student(@PathVariable Integer idPrijava) {
        return detaljiService.detalji(idPrijava);
    }

    @PostMapping("/student/{idPrijava}/napomena")
    public Map<String, Object> napomena(@PathVariable Integer idPrijava, @Valid @RequestBody NovaNapomenaDTO z) {
        return Map.of("poslano", detaljiService.posaljiNapomenu(idPrijava, z.tekst()));
    }
}