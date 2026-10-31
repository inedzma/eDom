package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.bodovanje.BodovanjeDetaljnoDTO;
import ba.unze.edom.common.dto.bodovanje.BodovanjeIzmjenaDTO;
import ba.unze.edom.common.dto.dokumenti.DokumentPapirDTO;
import ba.unze.edom.common.dto.dokumenti.DokumentSadrzajDTO;
import ba.unze.edom.common.dto.dokumenti.DokumentiSpisakDTO;
import ba.unze.edom.common.dto.dokumenti.DostavljenDTO;
import ba.unze.edom.common.dto.prijava.*;
import ba.unze.edom.server.service.bodovanje.BodovanjeAdminService;
import ba.unze.edom.server.service.dokumenti.AdminDokumentiService;
import ba.unze.edom.server.service.dokumenti.PotrebniDokumentiService;
import ba.unze.edom.server.service.prijava.NapomenaPrijaveService;
import ba.unze.edom.server.service.prijava.PrijavaAdminService;
import ba.unze.edom.server.service.prijava.RucniUnosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prijave")
@RequiredArgsConstructor
public class PrijavaApiController {

    private final PrijavaAdminService prijavaAdminService;
    private final BodovanjeAdminService bodovanjeAdminService;
    private final AdminDokumentiService adminDokumentiService;
    private final PotrebniDokumentiService potrebniDokumentiService;
    private final NapomenaPrijaveService napomenaPrijaveService;

    /** GET /api/prijave            -> zadnja akademska godina
     *  GET /api/prijave?godina=2026 -> zadana godina */
    @GetMapping
    public List<PrijavaListaDTO> lista(@RequestParam(required = false) Integer godina) {
        return prijavaAdminService.lista(godina);
    }

    @GetMapping("/{id}")
    public PrijavaDetaljiDTO detalji(@PathVariable Integer id) {
        return prijavaAdminService.detalji(id);
    }

    @GetMapping("/{idPrijave}/dokumenti/{idDokumenta}/sadrzaj")
    public DokumentSadrzajDTO sadrzaj(@PathVariable Integer idPrijave,
                                      @PathVariable Integer idDokumenta) {
        return new DokumentSadrzajDTO(
                prijavaAdminService.sadrzajDokumenta(idPrijave, idDokumenta));
    }

    private final RucniUnosService rucniUnosService;

    @PostMapping("/rucni-unos")
    public RucniUnosRezultatDTO rucniUnos(@Valid @RequestBody RucniUnosDTO zahtjev) {
        return rucniUnosService.unesi(zahtjev);
    }

    @PutMapping("/{id}/verifikacija")
    public PrijavaDetaljiDTO verifikuj(@PathVariable Integer id, @RequestBody VerifikacijaDTO z) {
        return prijavaAdminService.verifikuj(id, z);
    }

    @PutMapping("/{id}/status")
    public PrijavaDetaljiDTO promijeniStatus(@PathVariable Integer id,
                                             @Valid @RequestBody PromjenaStatusaDTO z) {
        return prijavaAdminService.promijeniStatus(id, z);
    }

    @GetMapping("/{id}/bodovanje")
    public BodovanjeDetaljnoDTO bodovanje(@PathVariable Integer id) {
        return bodovanjeAdminService.detaljno(id);
    }

    @PutMapping("/{id}/bodovanje")
    public BodovanjeDetaljnoDTO sacuvajBodovanje(@PathVariable Integer id, @RequestBody BodovanjeIzmjenaDTO z) {
        return bodovanjeAdminService.sacuvaj(id, z);
    }

    @GetMapping("/{id}/dokumenti-spisak")
    public DokumentiSpisakDTO dokumentiSpisak(@PathVariable Integer id) {
        return potrebniDokumentiService.zaAdmina(id);
    }

    @PutMapping("/{id}/dokumenti/{idDok}/dostavljen")
    public java.util.Map<String, Boolean> dostavljen(@PathVariable Integer id, @PathVariable Integer idDok,
                                                     @RequestBody DostavljenDTO z) {
        adminDokumentiService.dostavljen(id, idDok, z.dostavljen());
        return java.util.Map.of("ok", true);
    }

    @PostMapping("/{id}/dokumenti/papir")
    public java.util.Map<String, Boolean> dodajPapir(@PathVariable Integer id, @RequestBody DokumentPapirDTO z) {
        adminDokumentiService.dodajPapir(id, z);
        return java.util.Map.of("ok", true);
    }

    @PostMapping("/{id}/dokumenti/{idDok}/obrisi")
    public java.util.Map<String, Boolean> obrisiPapir(@PathVariable Integer id, @PathVariable Integer idDok) {
        adminDokumentiService.obrisiPapir(id, idDok);
        return java.util.Map.of("ok", true);
    }

    @PutMapping("/{id}/napomena")
    public java.util.Map<String, Boolean> napomena(@PathVariable Integer id, @RequestBody NapomenaPrijaveDTO z) {
        napomenaPrijaveService.sacuvaj(id, z.napomena());
        return java.util.Map.of("ok", true);
    }
}