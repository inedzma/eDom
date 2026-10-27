package ba.unze.edom.server.web;

import ba.unze.edom.common.util.Fakulteti;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Šifarnici koje koriste web forme (registracija, prijava). */
@ControllerAdvice
public class SifarniciWebAdvice {

    @ModelAttribute("fakulteti")
    public List<String> fakulteti() {
        return Fakulteti.SVI;
    }
}