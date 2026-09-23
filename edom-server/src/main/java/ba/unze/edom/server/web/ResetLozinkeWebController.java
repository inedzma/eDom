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
        try {
            resetService.zatraziKod(email);
        } catch (RuntimeException e) {
            model.addAttribute("greska", e.getMessage());
            model.addAttribute("email", email);
            return "reset/zahtjev";          // ostaje na istoj stranici
        }
        model.addAttribute("email", email);
        model.addAttribute("info", "Kod je poslan na " + email + ".");
        return "reset/potvrda";
    }

    @PostMapping("/potvrda")
    public String potvrdi(@RequestParam String email,
                          @RequestParam String kod,
                          @RequestParam String lozinka,
                          @RequestParam String potvrda,
                          Model model) {
        System.out.println(">>> KONTROLER potvrdi: email=" + email + " kod=" + kod);
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