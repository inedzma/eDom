package ba.unze.edom.server.web;

import ba.unze.edom.server.dto.KontaktForma;
import ba.unze.edom.server.service.KontaktPorukaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Slanje poruke s javne stranice Kontakt (GET /kontakt je u StraniceController). */
@Controller
@RequiredArgsConstructor
public class KontaktController {

    private final KontaktPorukaService kontaktService;

    @PostMapping("/kontakt")
    public String posalji(@ModelAttribute KontaktForma forma, HttpServletRequest req, RedirectAttributes ra) {
        try {
            kontaktService.posalji(forma, req.getRemoteAddr());
            ra.addFlashAttribute("kontaktPoslano", true);
        } catch (KontaktPorukaService.KontaktGreska e) {
            ra.addFlashAttribute("kontaktGreska", e.getMessage());
            ra.addFlashAttribute("forma", forma);       // da se ne kuca sve ponovo
        }
        return "redirect:/kontakt#poruka";
    }
}