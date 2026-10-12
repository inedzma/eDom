package api;

import service.Session;

public final class AuthApi {

    public record LoginZahtjev(String username, String lozinka) {}
    public record LoginOdgovor(String token) {}

    private AuthApi() {}

    public static void prijavi(String username, String lozinka) {
        LoginOdgovor o = ApiClient.post("/api/auth/login",
                new LoginZahtjev(username, lozinka), LoginOdgovor.class);
        Session.prijavi(username, o.token());
    }
}