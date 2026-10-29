package ba.unze.edom.server.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Provjera uloge koja ne zavisi od velikih/malih slova (ROLE_ADMIN, ROLE_Admin, ADMIN…). */
public final class Uloge {

    private Uloge() {}

    public static boolean jeAdmin(Authentication a) {
        if (a == null || !a.isAuthenticated()) return false;
        return a.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.regionMatches(true, 0, "ROLE_", 0, 5) ? r.substring(5) : r)
                .anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ADMINISTRATOR"));
    }
}