package ba.unze.edom.server.exception;

public class NevalidnaPrijavaException extends RuntimeException {
    public NevalidnaPrijavaException(String poruka) {
        super(poruka);
    }
}