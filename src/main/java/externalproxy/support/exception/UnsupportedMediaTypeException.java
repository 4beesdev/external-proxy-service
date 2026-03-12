package externalproxy.support.exception;

public class UnsupportedMediaTypeException extends RuntimeException {
    public UnsupportedMediaTypeException(String contentType) {
        super("Unsupported media type: " + contentType);
    }
}

