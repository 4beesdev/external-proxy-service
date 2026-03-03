package externalproxy.controller;

import externalproxy.domain.dto.CreateReviewRequest;
import externalproxy.domain.dto.ReviewAverageResponse;
import externalproxy.domain.dto.ReviewResponse;
import externalproxy.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;


    @PostMapping()
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request, HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request,httpServletRequest));
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


}
