package externalproxy.support.exception;

public class TooManyReviewsException extends RuntimeException {
    public TooManyReviewsException() {
        super("Max 2 reviews per IP");
    }
}

