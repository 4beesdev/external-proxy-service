package externalproxy.support.exception;

public class AlreadyLikedException extends RuntimeException {
    public AlreadyLikedException() {
        super("Already liked from this IP");
    }
}

