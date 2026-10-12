package api;

public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String poruka) {
        super(poruka);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}