package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.opste.ObavijestDTO;
import ba.unze.edom.server.service.obavijesti.ObavijestiAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/obavijesti")
@RequiredArgsConstructor
public class ObavijestiApiController {

    private final ObavijestiAdminService obavijestiService;

    @GetMapping
    public List<ObavijestDTO> sve() {
        return obavijestiService.sve();
    }
}