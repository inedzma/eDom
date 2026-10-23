package ba.unze.edom.server.web;

import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.security.KorisnikPrincipal;
import ba.unze.edom.server.service.PorukeService;
import ba.unze.edom.server.service.ProfilService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Kontakt forma: poruka administraciji doma. */
@Controller
@RequestMapping("/student/kontakt")
@RequiredArgsConstructor
public class StudentKontaktController {

    private final PorukeService porukeService;
    private final ProfilService profilService;

    @GetMapping
    public String forma(@AuthenticationPrincipal KorisnikPrincipal k, Model model) {
        var profil = profilService.ucitaj(k.getIdStudenta());
        model.addAttribute("ime", profil.ime());
        model.addAttribute("email", profil.email());
        return "student/kontakt";
    }

    @PostMapping
    public String posalji(@AuthenticationPrincipal KorisnikPrincipal k,
                          @RequestParam(required = false) String naslov,
                          @RequestParam String tekst,
                          RedirectAttributes ra) {
        try {
            porukeService.posalji(k.getIdStudenta(), naslov, tekst);
            ra.addFlashAttribute("poruka", "Poruka je poslana. Odgovor ćete dobiti na svoj e-mail.");
        } catch (PrijavaException e) {
            ra.addFlashAttribute("greska", e.getMessage());
            ra.addFlashAttribute("naslov", naslov);
            ra.addFlashAttribute("tekst", tekst);
        }
        return "redirect:/student/kontakt";
    }
}