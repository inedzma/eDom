package ba.unze.edom.server.security;

import ba.unze.edom.server.repository.KorisnikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KorisnikDetailsService implements UserDetailsService {

    private final KorisnikRepository korisnikRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {

        var k = korisnikRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Korisnik ne postoji"));

        String nazivUloge = (k.getUloga() != null && k.getUloga().getNaziv() != null)
                ? k.getUloga().getNaziv()
                : "Nepoznato";

        return new KorisnikPrincipal(
                k.getIdKorisnik(),
                k.getUsername(),
                k.getPasswordHash(),
                k.getStudent() != null ? k.getStudent().getIdStudent() : null,
                List.of(new SimpleGrantedAuthority("ROLE_" + nazivUloge))
        );
    }
}