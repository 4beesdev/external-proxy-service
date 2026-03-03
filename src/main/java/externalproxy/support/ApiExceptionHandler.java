package externalproxy.support;

import externalproxy.support.exception.AlreadyLikedException;
import externalproxy.support.exception.ReviewInvalidStateException;
import externalproxy.support.exception.ReviewNotFoundException;
import externalproxy.support.exception.ReviewNotApprovedException;
import externalproxy.support.exception.TooManyReviewsException;
import externalproxy.support.exception.TotalMediaSizeExceededException;
import externalproxy.support.exception.UnsupportedMediaTypeException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TooManyReviewsException.class)
    public ResponseEntity<ApiErrorResponse> tooManyReviews(TooManyReviewsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiErrorResponse.of("TOO_MANY_REVIEWS", ex.getMessage()));
    }

    @ExceptionHandler(AlreadyLikedException.class)
    public ResponseEntity<ApiErrorResponse> alreadyLiked(AlreadyLikedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ALREADY_LIKED", ex.getMessage()));
    }

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> reviewNotFound(ReviewNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("REVIEW_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ReviewNotApprovedException.class)
    public ResponseEntity<ApiErrorResponse> reviewNotApproved(ReviewNotApprovedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("REVIEW_NOT_APPROVED", ex.getMessage()));
    }

    @ExceptionHandler(ReviewInvalidStateException.class)
    public ResponseEntity<ApiErrorResponse> reviewInvalidState(ReviewInvalidStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("REVIEW_INVALID_STATE", ex.getMessage()));
    }

    @ExceptionHandler(TotalMediaSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> totalMediaTooLarge(TotalMediaSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiErrorResponse.of("REVIEW_MEDIA_TOTAL_LIMIT_EXCEEDED", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> uploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiErrorResponse.of("PAYLOAD_TOO_LARGE", "Upload exceeds configured limit"));
    }

    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ResponseEntity<ApiErrorResponse> unsupportedMediaType(UnsupportedMediaTypeException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of("UNSUPPORTED_MEDIA_TYPE", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.withFieldErrors("VALIDATION_ERROR", "Invalid request", fieldErrors));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> forbidden(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of("FORBIDDEN", ex.getMessage()));
    }
}

