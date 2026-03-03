package externalproxy.support.exception;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(long id) {
        super("Review not found: " + id);
    }
}

