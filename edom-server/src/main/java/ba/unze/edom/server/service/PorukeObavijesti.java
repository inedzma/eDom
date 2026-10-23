package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.ObavijestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(4)
@RequiredArgsConstructor
public class PorukeObavijesti implements IzvorObavijesti {

    private final PorukeService porukeService;

    @Override
    public List<ObavijestDTO> obavijesti() {
        return porukeService.nepročitane().stream().map(p -> new ObavijestDTO(
                "PORUKA",
                "PORUKA:" + p.id(),
                p.naslov() + " — " + p.ime(),
                p.tekst().length() > 160 ? p.tekst().substring(0, 159) + "…" : p.tekst(),
                p.vrijeme().toLocalDate(),
                null,
                null)).toList();
    }
}