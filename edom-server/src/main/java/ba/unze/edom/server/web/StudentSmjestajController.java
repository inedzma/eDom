package ba.unze.edom.server.web;

import ba.unze.edom.server.dto.ZeljaForma;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.sobe.StudentSmjestajService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Stranica "Moj smještaj": soba i cimeri, te želja (prije dodjele ili kao zahtjev za promjenu). */
@Controller
@RequestMapping("/student/smjestaj")
@RequiredArgsConstructor
public class StudentSmjestajController {

    private final StudentSmjestajService service;

    @GetMapping
    public String stranica(@AuthenticationPrincipal KorisnikPrincipal k, Model model) {
        model.addAttribute("stanje", service.stanje(k.getIdStudenta()).orElse(null));
        if (!model.containsAttribute("zelja")) model.addAttribute("zelja", service.zelja(k.getIdStudenta()));
        return "student/smjestaj";
    }

    @PostMapping("/zelja")
    public String sacuvajZelju(@AuthenticationPrincipal KorisnikPrincipal k,
                               @ModelAttribute("zelja") ZeljaForma forma,
                               RedirectAttributes ra) {
        try {
            service.sacuvajZelju(k.getIdStudenta(), forma);
            ra.addFlashAttribute("poruka", "Sačuvano. Administracija će razmotriti vaše želje.");
        } catch (PrijavaException e) {
            ra.addFlashAttribute("greska", e.getMessage());
            ra.addFlashAttribute("zelja", forma);
        }
        return "redirect:/student/smjestaj#zelja";
    }
}