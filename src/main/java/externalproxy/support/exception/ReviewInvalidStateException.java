package externalproxy.support.exception;

public class ReviewInvalidStateException extends RuntimeException {
    public ReviewInvalidStateException(long reviewId, String message) {
        super("Review " + reviewId + ": " + message);
    }
}

