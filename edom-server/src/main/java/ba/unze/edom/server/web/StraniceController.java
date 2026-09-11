package ba.unze.edom.server.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StraniceController {

    @GetMapping("/")
    public String pocetna(Authentication auth) {
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/student/pocetna";
        }
        return "pocetna";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}