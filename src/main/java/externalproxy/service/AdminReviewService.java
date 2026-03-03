package externalproxy.service;

import externalproxy.domain.Admin;
import externalproxy.domain.Review;
import externalproxy.domain.dto.AdminReviewResponse;
import externalproxy.domain.enumeration.Role;
import externalproxy.domain.enumeration.ReviewStatus;
import externalproxy.repository.ReviewLikeCount;
import externalproxy.repository.ReviewLikeRepository;
import externalproxy.repository.ReviewRepository;
import externalproxy.support.exception.ReviewInvalidStateException;
import externalproxy.support.exception.ReviewNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final AdminService adminService;

    @Transactional(readOnly = true)
    public List<AdminReviewResponse> list(Authentication authentication) {
        Admin admin = requireAdmin(authentication);
        List<Review> reviews = reviewRepository.findAllNotDeletedOrderByCreatedAtDesc();
        Map<Long, Long> likeCounts = likeCountsForReviews(reviews);

        return reviews.stream()
                .map(r -> new AdminReviewResponse(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getUsername(),
                        r.getEmail(),
                        r.getCreatedAt(),
                        likeCounts.getOrDefault(r.getId(), 0L),
                        r.getStatus(),
                        r.getAdminReply(),
                        r.getApprovedAt(),
                        r.getApprovedBy() == null ? null : r.getApprovedBy().getId(),
                        r.getRepliedAt(),
                        r.getRepliedBy() == null ? null : r.getRepliedBy().getId(),
                        r.getDeletedAt(),
                        r.getDeletedBy() == null ? null : r.getDeletedBy().getId()
                ))
                .toList();
    }

    @Transactional
    public void approve(Authentication authentication, long reviewId) {
        Admin admin = requireAdmin(authentication);
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ReviewInvalidStateException(reviewId, "Cannot approve deleted review");
        }
        if (review.getStatus() == ReviewStatus.APPROVED) {
            return;
        }

        review.setStatus(ReviewStatus.APPROVED);
        review.setApprovedAt(LocalDateTime.now());
        review.setApprovedBy(admin);
        reviewRepository.save(review);
    }

    @Transactional
    public void reply(Authentication authentication, long reviewId, String reply) {
        Admin admin = requireAdmin(authentication);
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ReviewInvalidStateException(reviewId, "Cannot reply to deleted review");
        }

        review.setAdminReply(reply);
        review.setRepliedAt(LocalDateTime.now());
        review.setRepliedBy(admin);
        reviewRepository.save(review);
    }

    @Transactional
    public void softDelete(Authentication authentication, long reviewId) {
        Admin admin = requireAdmin(authentication);
        Review review = reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (review.getStatus() == ReviewStatus.DELETED) {
            return;
        }

        review.setStatus(ReviewStatus.DELETED);
        review.setDeletedAt(LocalDateTime.now());
        review.setDeletedBy(admin);
        reviewRepository.save(review);
    }

    private Admin requireAdmin(Authentication authentication) {
        Admin admin = adminService.loadUserByUsername(authentication.getName());
        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("INSUFFICIENT_PERMISSIONS");
        }
        return admin;
    }

    private Map<Long, Long> likeCountsForReviews(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        List<ReviewLikeCount> counts = reviewLikeRepository.countByReviewIds(ids);
        return counts.stream().collect(Collectors.toMap(ReviewLikeCount::getReviewId, ReviewLikeCount::getCnt));
    }
}

