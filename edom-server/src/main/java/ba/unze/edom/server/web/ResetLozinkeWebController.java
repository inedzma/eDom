package ba.unze.edom.server.web;

import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.service.ResetLozinkeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/zaboravljena-lozinka")
@RequiredArgsConstructor
public class ResetLozinkeWebController {

    private final ResetLozinkeService resetService;

    @GetMapping
    public String forma() {
        return "reset/zahtjev";
    }

    @PostMapping
    public String posaljiKod(@RequestParam String email, Model model) {
        resetService.zatraziKod(email);
        // uvijek ista poruka i isti sljedeci korak
        model.addAttribute("email", email);
        model.addAttribute("info",
                "Ako postoji nalog sa tom adresom, poslali smo kod. "
                        + "Provjerite i neželjenu poštu.");
        return "reset/potvrda";
    }

    @PostMapping("/potvrda")
    public String potvrdi(@RequestParam String email,
                          @RequestParam String kod,
                          @RequestParam String lozinka,
                          @RequestParam String potvrda,
                          Model model) {
        try {
            resetService.promijeniLozinku(email, kod, lozinka, potvrda);
            return "redirect:/login?resetovano";
        } catch (RegistracijaException e) {
            model.addAttribute("email", email);
            model.addAttribute("greska", e.getMessage());
            return "reset/potvrda";
        }
    }
}