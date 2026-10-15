package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.*;
import ba.unze.edom.server.service.PrijavaAdminService;
import ba.unze.edom.server.service.RucniUnosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prijave")
@RequiredArgsConstructor
public class PrijavaApiController {

    private final PrijavaAdminService prijavaAdminService;

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
}