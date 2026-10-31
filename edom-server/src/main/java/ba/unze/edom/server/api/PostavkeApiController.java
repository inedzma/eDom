package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.bodovanje.KriterijPostavkaDTO;
import ba.unze.edom.common.dto.bodovanje.KriterijiPostavkeDTO;
import ba.unze.edom.common.dto.opste.PostavkeDTO;
import ba.unze.edom.server.service.bodovanje.KriterijiPostavkeService;
import ba.unze.edom.server.service.opste.PostavkeService;
import ba.unze.edom.server.service.bodovanje.PreracunBodovaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/postavke")
@RequiredArgsConstructor
public class PostavkeApiController {

    private final PostavkeService postavkeService;
    private final KriterijiPostavkeService kriterijiPostavkeService;
    private final PreracunBodovaService preracun;

    @GetMapping
    public PostavkeDTO ucitaj() {
        return postavkeService.trenutne();
    }

    @PutMapping
    public PostavkeDTO sacuvaj(@RequestBody PostavkeDTO z) {
        PostavkeDTO p = postavkeService.sacuvaj(z);
        preracun.preracunaj();          // pravila bodovanja su možda promijenjena
        return p;
    }

    @GetMapping("/kriteriji")
    public KriterijiPostavkeDTO kriteriji() {
        return kriterijiPostavkeService.lista();
    }

    @PutMapping("/kriteriji")
    public KriterijiPostavkeDTO sacuvajKriterij(@RequestBody KriterijPostavkaDTO z) {
        KriterijiPostavkeDTO l = kriterijiPostavkeService.sacuvaj(z);
        preracun.preracunaj();
        return l;
    }

    @PostMapping("/kriteriji/{id}/obrisi")
    public KriterijiPostavkeDTO obrisiKriterij(@PathVariable Integer id) {
        return kriterijiPostavkeService.obrisi(id);
    }
}