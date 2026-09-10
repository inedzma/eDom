package ba.unze.edom.server.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

@Getter
public class KorisnikPrincipal implements UserDetails {

    private final Integer idKorisnik;
    private final String username;
    private final String password;
    private final Integer idStudenta;
    private final Collection<? extends GrantedAuthority> authorities;

    public KorisnikPrincipal(Integer idKorisnik, String username, String password,
                             Integer idStudenta,
                             Collection<? extends GrantedAuthority> authorities) {
        this.idKorisnik = idKorisnik;
        this.username = username;
        this.password = password;
        this.idStudenta = idStudenta;
        this.authorities = authorities;
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}