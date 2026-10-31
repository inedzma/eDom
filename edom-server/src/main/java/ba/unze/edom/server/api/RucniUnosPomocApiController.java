package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.bodovanje.RezultatBodovanja;
import ba.unze.edom.common.dto.prijava.RucniUnosDTO;
import ba.unze.edom.common.dto.prijava.SpisakUnosDTO;
import ba.unze.edom.common.dto.prijava.SpisakUnosZahtjevDTO;
import ba.unze.edom.server.service.dokumenti.PotrebniDokumentiService;
import ba.unze.edom.server.service.prijava.RucniUnosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Pomoć desktopu tokom unosa: spisak dokumenata i bodovi prije snimanja. */
@RestController
@RequestMapping("/api/prijave/rucni-unos")
@RequiredArgsConstructor
public class RucniUnosPomocApiController {

    private final PotrebniDokumentiService potrebniDokumenti;
    private final RucniUnosService rucniUnos;

    @PostMapping("/spisak-dokumenata")
    public SpisakUnosDTO spisakDokumenata(@Valid @RequestBody SpisakUnosZahtjevDTO z) {
        return potrebniDokumenti.zaUnos(z);
    }

    @PostMapping("/pregled-bodova")
    public RezultatBodovanja pregledBodova(@Valid @RequestBody RucniUnosDTO z) {
        return rucniUnos.pregledBodova(z);
    }
}