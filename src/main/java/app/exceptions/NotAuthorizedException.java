package app.exceptions;

public class NotAuthorizedException extends RuntimeException {
    private final int status;

    public NotAuthorizedException(String message, int status) {
        super(message);
        this.status = status;
    }
    public int getStatus() { return status; }

    public static NotAuthorizedException unauthorized(String msg) {
        return new NotAuthorizedException(msg, 401);
    }
}
