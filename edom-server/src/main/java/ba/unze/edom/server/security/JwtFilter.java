package ba.unze.edom.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final KorisnikDetailsService detailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest zahtjev,
                                    HttpServletResponse odgovor,
                                    FilterChain lanac)
            throws ServletException, IOException {

        String header = zahtjev.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtService.validan(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                var korisnik = detailsService.loadUserByUsername(
                        jwtService.izvuciUsername(token));

                var auth = new UsernamePasswordAuthenticationToken(
                        korisnik, null, korisnik.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        lanac.doFilter(zahtjev, odgovor);
    }
}