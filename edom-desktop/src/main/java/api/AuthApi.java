package api;

import ba.unze.edom.common.dto.korisnik.LoginOdgovor;
import ba.unze.edom.common.dto.korisnik.LoginZahtjev;
import service.Session;

public final class AuthApi {

    private AuthApi() {}

    public static void prijavi(String username, String lozinka) {
        LoginOdgovor o = ApiClient.post("/api/auth/login",
                new LoginZahtjev(username, lozinka), LoginOdgovor.class);
        Session.prijavi(username, o.token());
    }
}