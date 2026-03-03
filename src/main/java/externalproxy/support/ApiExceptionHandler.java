package externalproxy.support;

import externalproxy.support.exception.AlreadyLikedException;
import externalproxy.support.exception.ReviewInvalidStateException;
import externalproxy.support.exception.ReviewNotFoundException;
import externalproxy.support.exception.ReviewNotApprovedException;
import externalproxy.support.exception.TooManyReviewsException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

