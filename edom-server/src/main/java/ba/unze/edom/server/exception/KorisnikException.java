package ba.unze.edom.server.exception;

public class KorisnikException extends RuntimeException {
    public KorisnikException(String poruka) {
        super(poruka);
    }
}