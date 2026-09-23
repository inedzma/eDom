package ba.unze.edom.server.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PrijavaUspjeh implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest zahtjev,
                                        HttpServletResponse odgovor,
                                        Authentication auth)
            throws IOException, ServletException {

        boolean student = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_STUDENT"));

        if (student) {
            odgovor.sendRedirect(zahtjev.getContextPath() + "/student/pocetna");
            return;
        }

        // administrator koristi desktop aplikaciju - prekini sesiju
        HttpSession s = zahtjev.getSession(false);
        if (s != null) s.invalidate();

        odgovor.sendRedirect(zahtjev.getContextPath() + "/login?samo-studenti");
    }
}