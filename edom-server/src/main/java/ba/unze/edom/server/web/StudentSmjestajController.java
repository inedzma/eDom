package ba.unze.edom.server.web;

import ba.unze.edom.server.dto.ZahtjevForma;
import ba.unze.edom.server.dto.ZeljaForma;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.security.KorisnikPrincipal;   // uskladi s PrijavaWebController
import ba.unze.edom.server.service.StudentSmjestajService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Stranica "Moj smještaj": želje prije dodjele, soba i zahtjev za promjenu poslije. */
@Controller
@RequestMapping("/student/smjestaj")
@RequiredArgsConstructor
public class StudentSmjestajController {

    private final StudentSmjestajService service;

    @GetMapping
    public String stranica(@AuthenticationPrincipal KorisnikPrincipal k, Model model) {
        model.addAttribute("stanje", service.stanje(k.getIdStudenta()).orElse(null));
        if (!model.containsAttribute("zelja")) model.addAttribute("zelja", service.zelja(k.getIdStudenta()));
        if (!model.containsAttribute("zahtjev")) model.addAttribute("zahtjev", new ZahtjevForma());
        return "student/smjestaj";
    }

    @PostMapping("/zelja")
    public String sacuvajZelju(@AuthenticationPrincipal KorisnikPrincipal k,
                               @ModelAttribute("zelja") ZeljaForma forma,
                               RedirectAttributes ra) {
        try {
            service.sacuvajZelju(k.getIdStudenta(), forma);
            ra.addFlashAttribute("poruka", "Vaše želje su sačuvane.");
        } catch (PrijavaException e) {
            ra.addFlashAttribute("greska", e.getMessage());
            ra.addFlashAttribute("zelja", forma);
        }
        return "redirect:/student/smjestaj";
    }

    @PostMapping("/zahtjev")
    public String podnesiZahtjev(@AuthenticationPrincipal KorisnikPrincipal k,
                                 @ModelAttribute("zahtjev") ZahtjevForma forma,
                                 RedirectAttributes ra) {
        try {
            service.podnesiZahtjev(k.getIdStudenta(), forma);
            ra.addFlashAttribute("poruka", "Zahtjev je podnesen. Administracija će ga pregledati.");
        } catch (PrijavaException e) {
            ra.addFlashAttribute("greska", e.getMessage());
            ra.addFlashAttribute("zahtjev", forma);
        }
        return "redirect:/student/smjestaj#zahtjev";
    }

    @PostMapping("/zahtjev/{id}/povuci")
    public String povuciZahtjev(@AuthenticationPrincipal KorisnikPrincipal k,
                                @PathVariable Integer id,
                                RedirectAttributes ra) {
        try {
            service.povuciZahtjev(k.getIdStudenta(), id);
            ra.addFlashAttribute("poruka", "Zahtjev je povučen.");
        } catch (PrijavaException e) {
            ra.addFlashAttribute("greska", e.getMessage());
        }
        return "redirect:/student/smjestaj#zahtjev";
    }
}