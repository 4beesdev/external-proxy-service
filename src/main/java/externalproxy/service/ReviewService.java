package externalproxy.service;

import externalproxy.domain.Admin;
import externalproxy.domain.Review;
import externalproxy.domain.ReviewLike;
import externalproxy.domain.dto.CreateReviewRequest;
import externalproxy.domain.dto.ReviewAverageResponse;
import externalproxy.domain.dto.ReviewResponse;
import externalproxy.domain.enumeration.Role;
import externalproxy.domain.enumeration.ReviewStatus;
import externalproxy.repository.RatingCount;
import externalproxy.repository.ReviewLikeCount;
import externalproxy.repository.ReviewLikeRepository;
import externalproxy.repository.ReviewRepository;
import externalproxy.support.ClientIpResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import externalproxy.support.AdvisoryLockService;
import externalproxy.support.IpHashService;
import externalproxy.support.exception.AlreadyLikedException;
import externalproxy.support.exception.ReviewNotFoundException;
import externalproxy.support.exception.TooManyReviewsException;
import externalproxy.support.exception.ReviewNotApprovedException;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final AdvisoryLockService advisoryLockService;
    private final IpHashService ipHashService;
    private final ClientIpResolver clientIpResolver;
    private final AdminService adminService;

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request, HttpServletRequest httpServletRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpServletRequest);
        String ipHash = ipHashService.hashIp(clientIp);
        advisoryLockService.lock(ipHash);

        long existing = reviewRepository.countByIpHash(ipHash);
        if (existing >= 2) {
            throw new TooManyReviewsException();
        }

        Review review = new Review();
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUsername(request.getUsername());
        review.setEmail(request.getEmail());
        review.setIpHash(ipHash);
        review.setStatus(ReviewStatus.PENDING);

        Review saved = reviewRepository.save(review);
        return new ReviewResponse(saved.getId(), saved.getRating(), saved.getComment(), saved.getUsername(), saved.getCreatedAt(), 0L, null);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listReviews() {
        List<Review> reviews = reviewRepository.findAllByStatusOrderByCreatedAtDesc(ReviewStatus.APPROVED);
        Map<Long, Long> likeCounts = likeCountsForReviews(reviews);

        return reviews.stream()
                .map(r -> new ReviewResponse(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getUsername(),
                        r.getCreatedAt(),
                        likeCounts.getOrDefault(r.getId(), 0L),
                        r.getAdminReply()
                ))
                .toList();
    }

    @Transactional
    public void likeReview(long reviewId, HttpServletRequest httpServletRequest) {
        String clientIp = clientIpResolver.resolveClientIp(httpServletRequest);
        String ipHash = ipHashService.hashIp(clientIp);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (review.getStatus() != ReviewStatus.APPROVED) {
            throw new ReviewNotApprovedException(reviewId);
        }

        ReviewLike rl = new ReviewLike();
        rl.setReview(review);
        rl.setIpHash(ipHash);

        try {
            reviewLikeRepository.saveAndFlush(rl);
        } catch (DataIntegrityViolationException e) {
            throw new AlreadyLikedException();
        }
    }

    @Transactional(readOnly = true)
    public ReviewAverageResponse getAverage() {
        long total = reviewRepository.countByStatus(ReviewStatus.APPROVED);
        Double avg = reviewRepository.getAverageRatingByStatus(ReviewStatus.APPROVED);
        double average = avg == null ? 0.0 : avg;

        Map<Integer, Long> ratingCounts = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingCounts.put(i, 0L);
        }
        List<RatingCount> counts = reviewRepository.countByRatingForStatus(ReviewStatus.APPROVED);
        for (RatingCount rc : counts) {
            if (rc.getRating() != null && rc.getCnt() != null) {
                ratingCounts.put(rc.getRating(), rc.getCnt());
            }
        }

        return new ReviewAverageResponse(average, total, ratingCounts);
    }

    private Map<Long, Long> likeCountsForReviews(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        List<ReviewLikeCount> counts = reviewLikeRepository.countByReviewIds(ids);
        return counts.stream().collect(Collectors.toMap(ReviewLikeCount::getReviewId, ReviewLikeCount::getCnt));
    }

    public void deleteReview(Authentication authentication, long id, HttpServletRequest httpServletRequest) {
        Admin admin = adminService.loadUserByUsername(authentication.getName());
        if(admin.getRole()!= Role.ROLE_ADMIN){
            throw new AccessDeniedException("INSUFFICIENT_PERMISSIONS");
        }
        Review review = reviewRepository.findById(id).orElseThrow(() -> new ReviewNotFoundException(id));
        if (review.getStatus() != ReviewStatus.DELETED) {
            review.setStatus(ReviewStatus.DELETED);
            review.setDeletedAt(java.time.LocalDateTime.now());
            review.setDeletedBy(admin);
            reviewRepository.save(review);
        }
    }
}
