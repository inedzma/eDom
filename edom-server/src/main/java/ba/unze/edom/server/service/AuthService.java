package ba.unze.edom.server.service;

import ba.unze.edom.server.repository.KorisnikRepository;
import ba.unze.edom.server.security.JwtService;
import ba.unze.edom.server.security.KorisnikPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final KorisnikRepository korisnikRepository;
    private final JwtService jwtService;

    @Transactional
    public String prijaviAdmina(String username, String lozinka) {

        var auth = autentifikuj(username, lozinka);
        var principal = (KorisnikPrincipal) auth.getPrincipal();

        boolean jeAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!jeAdmin) {
            throw new BadCredentialsException("Pristup je dozvoljen samo administratoru.");
        }

        zabiljeziPrijavu(principal.getIdKorisnik());
        return jwtService.generisi(username);
    }

    private org.springframework.security.core.Authentication autentifikuj(
            String username, String lozinka) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, lozinka));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Neispravni podaci za prijavu.");
        }
    }

    private void zabiljeziPrijavu(Integer idKorisnika) {
        korisnikRepository.findById(idKorisnika).ifPresent(k -> {
            k.setZadnjaPrijava(Instant.now());
            korisnikRepository.save(k);
        });
    }
}