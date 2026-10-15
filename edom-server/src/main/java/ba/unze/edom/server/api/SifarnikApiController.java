package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.SifarniciDTO;
import ba.unze.edom.server.service.SifarnikService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sifarnici")
@RequiredArgsConstructor
public class SifarnikApiController {

    private final SifarnikService sifarnikService;

    @GetMapping
    public SifarniciDTO svi() {
        return sifarnikService.svi();
    }
}