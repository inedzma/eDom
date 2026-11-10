package ba.unze.edom.server.web;

import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import ba.unze.edom.server.exception.PrijavaException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Poslovne greške na web stranicama (npr. tuđa ili nepostojeća prijava)
 * prikazuju se na stranici greške s porukom, umjesto "Whitelabel" stranice.
 * Kontroler koji ima svoj @ExceptionHandler (npr. StudentWebController) i dalje koristi svoj.
 * REST greške za desktop rješava ApiGreskeHandler.
 */
@ControllerAdvice(basePackages = "ba.unze.edom.server.web")
public class WebGreskeHandler {

    @ExceptionHandler({PrijavaException.class, NevalidnaPrijavaException.class})
    public ModelAndView poslovnaGreska(RuntimeException e) {
        ModelAndView mv = new ModelAndView("error");
        mv.setStatus(HttpStatus.BAD_REQUEST);
        mv.addObject("status", 400);
        mv.addObject("poruka", e.getMessage());
        return mv;
    }
}