package ba.unze.edom.server;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashTest {

    @Test
    void provjeri() {
        var enc = new BCryptPasswordEncoder();
        String hashIzBaze = "$2a$10$eSeysBwegi3foE/guqOCH.HPbZZAh55fBixlEJ6PPBX3CoZZIdRu2";

        System.out.println("nova lozinka:  " + enc.matches("Hntals.0106", hashIzBaze));
        System.out.println("stara lozinka: " + enc.matches("Nedzma.123", hashIzBaze));
        System.out.println("kod iz maila:  " + enc.matches("505581", hashIzBaze));
    }
}