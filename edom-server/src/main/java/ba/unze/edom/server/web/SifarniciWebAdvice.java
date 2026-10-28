package ba.unze.edom.server.web;

import ba.unze.edom.common.dto.PostavkeDTO;
import ba.unze.edom.common.util.Fakulteti;
import ba.unze.edom.server.service.PostavkeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Šifarnici i postavke za web šablone (registracija, prijava, rang lista, upute…). */
@ControllerAdvice(basePackages = "ba.unze.edom.server.web")
@RequiredArgsConstructor
public class SifarniciWebAdvice {

    private final PostavkeService postavkeService;

    @ModelAttribute("fakulteti")
    public List<String> fakulteti() {
        return Fakulteti.SVI;
    }

    @ModelAttribute("postavke")
    public PostavkeDTO postavke() {
        return postavkeService.trenutne();
    }

    @ModelAttribute("konkurs")
    public PostavkeService.KonkursInfo konkurs() {
        return postavkeService.konkurs();
    }
}