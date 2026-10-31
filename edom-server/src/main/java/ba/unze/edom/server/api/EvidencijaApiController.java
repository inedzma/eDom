package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.student.EvidencijaDTO;
import ba.unze.edom.common.dto.student.EvidencijaOznakeDTO;
import ba.unze.edom.common.dto.dokumenti.GenerisaniDokumentDTO;
import ba.unze.edom.server.service.student.EvidencijaService;
import ba.unze.edom.server.service.prijava.ObrasciService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/evidencija")
@RequiredArgsConstructor
public class EvidencijaApiController {

    private final EvidencijaService evidencijaService;
    private final ObrasciService obrasciService;

    @GetMapping
    public EvidencijaDTO lista(@RequestParam(required = false) Integer godina) {
        return evidencijaService.lista(godina);
    }

    @PutMapping("/{idPrijave}/oznake")
    public Map<String, Boolean> oznake(@PathVariable Integer idPrijave, @RequestBody EvidencijaOznakeDTO z) {
        evidencijaService.sacuvajOznake(idPrijave, z);
        return Map.of("ok", true);
    }

    @PostMapping("/{idPrijave}/ugovor")
    public GenerisaniDokumentDTO ugovor(@PathVariable Integer idPrijave) {
        return obrasciService.ugovor(idPrijave);
    }

    @PostMapping("/{idPrijave}/revers")
    public GenerisaniDokumentDTO revers(@PathVariable Integer idPrijave) {
        return obrasciService.revers(idPrijave);
    }
}