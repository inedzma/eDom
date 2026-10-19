package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.RezultatBodovanja;
import ba.unze.edom.common.dto.RucniUnosDTO;
import ba.unze.edom.common.dto.SpisakUnosDTO;
import ba.unze.edom.common.dto.SpisakUnosZahtjevDTO;
import ba.unze.edom.server.service.PotrebniDokumentiService;
import ba.unze.edom.server.service.RucniUnosService;
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