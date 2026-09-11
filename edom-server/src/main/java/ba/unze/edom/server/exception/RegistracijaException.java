package ba.unze.edom.server.exception;

public class RegistracijaException extends RuntimeException {

    public RegistracijaException(String poruka) {
        super(poruka);
    }
}