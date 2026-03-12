package externalproxy.support.exception;

public class ReviewNotApprovedException extends RuntimeException {
    public ReviewNotApprovedException(long reviewId) {
        super("Review is not approved: " + reviewId);
    }
}

