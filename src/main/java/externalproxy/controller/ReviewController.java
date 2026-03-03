package externalproxy.controller;

import externalproxy.domain.dto.CreateReviewRequest;
import externalproxy.domain.dto.ReviewAverageResponse;
import externalproxy.domain.dto.ReviewResponse;
import externalproxy.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.Valid;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponse> createReviewMultipart(@Valid @RequestPart("review") CreateReviewRequest request, @RequestPart(name = "files", required = false) List<MultipartFile> files, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReviewWithMedia(request, files, httpServletRequest));
    }

    @GetMapping()
    public List<ReviewResponse> listReviews() {
        return reviewService.listReviews();
    }

    @GetMapping("/average")
    public ReviewAverageResponse average() {
        return reviewService.getAverage();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeReview(@PathVariable("id") long id, HttpServletRequest httpServletRequest) {
        reviewService.likeReview(id, httpServletRequest);
        return ResponseEntity.noContent().build();
    }


    private String maskEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            return "<invalid>";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        String localMasked = local.length() <= 2
                ? local.charAt(0) + "***"
                : local.substring(0, 2) + "***";
        return localMasked + "@" + domain;
    }

}
