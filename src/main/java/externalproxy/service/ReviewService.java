package externalproxy.service;

import externalproxy.domain.Admin;
import externalproxy.domain.Review;
import externalproxy.domain.ReviewLike;
import externalproxy.domain.ReviewMedia;
import externalproxy.domain.dto.CreateReviewRequest;
import externalproxy.domain.dto.MediaResponse;
import externalproxy.domain.dto.ReviewAverageResponse;
import externalproxy.domain.dto.ReviewResponse;
import externalproxy.domain.enumeration.MediaKind;
import externalproxy.domain.enumeration.Role;
import externalproxy.domain.enumeration.ReviewStatus;
import externalproxy.repository.RatingCount;
import externalproxy.repository.ReviewLikeCount;
import externalproxy.repository.ReviewLikeRepository;
import externalproxy.repository.ReviewMediaRepository;
import externalproxy.repository.ReviewRepository;
import externalproxy.support.ClientIpResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import externalproxy.support.AdvisoryLockService;
import externalproxy.support.IpHashService;
import externalproxy.support.exception.AlreadyLikedException;
import externalproxy.support.exception.ReviewNotFoundException;
import externalproxy.support.exception.TooManyReviewsException;
import externalproxy.support.exception.ReviewNotApprovedException;
import externalproxy.support.exception.TotalMediaSizeExceededException;
import externalproxy.support.exception.UnsupportedMediaTypeException;

import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final AdvisoryLockService advisoryLockService;
    private final IpHashService ipHashService;
    private final ClientIpResolver clientIpResolver;
    private final AdminService adminService;

    private final long maxTotalMediaBytes;

    public ReviewService(
            ReviewRepository reviewRepository,
            ReviewLikeRepository reviewLikeRepository,
            ReviewMediaRepository reviewMediaRepository,
            AdvisoryLockService advisoryLockService,
            IpHashService ipHashService,
            ClientIpResolver clientIpResolver,
            AdminService adminService,
            @Value("${reviews.media.max-total-bytes:104857600}") long maxTotalMediaBytes
    ) {
        this.reviewRepository = reviewRepository;
        this.reviewLikeRepository = reviewLikeRepository;
        this.reviewMediaRepository = reviewMediaRepository;
        this.advisoryLockService = advisoryLockService;
        this.ipHashService = ipHashService;
        this.clientIpResolver = clientIpResolver;
        this.adminService = adminService;
        this.maxTotalMediaBytes = maxTotalMediaBytes;
    }

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
        return new ReviewResponse(saved.getId(), saved.getRating(), saved.getComment(), saved.getUsername(), saved.getCreatedAt(), 0L, null, List.of());
    }

    @Transactional
    public ReviewResponse createReviewWithMedia(CreateReviewRequest request, List<MultipartFile> files, HttpServletRequest httpServletRequest) {
        long totalBytes = 0L;
        if (files != null) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) {
                    continue;
                }
                totalBytes += f.getSize();
            }
        }
        if (totalBytes > maxTotalMediaBytes) {
            throw new TotalMediaSizeExceededException(totalBytes, maxTotalMediaBytes);
        }

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

        List<MediaResponse> mediaResponses = new ArrayList<>();
        if (files != null) {
            int sort = 0;
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) {
                    continue;
                }
                String contentType = f.getContentType();
                MediaKind kind = kindForContentType(contentType);

                ReviewMedia media = new ReviewMedia();
                media.setReview(saved);
                media.setKind(kind);
                media.setFilename(f.getOriginalFilename());
                media.setContentType(contentType);
                media.setSizeBytes(f.getSize());
                media.setSortOrder(sort++);
                try {
                    media.setData(f.getBytes());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to read uploaded file bytes", e);
                }

                ReviewMedia persisted = reviewMediaRepository.save(media);
                mediaResponses.add(toMediaResponse(persisted));
            }
        }

        return new ReviewResponse(saved.getId(), saved.getRating(), saved.getComment(), saved.getUsername(), saved.getCreatedAt(), 0L, null, mediaResponses);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listReviews() {
        List<Review> reviews = reviewRepository.findAllByStatusOrderByCreatedAtDesc(ReviewStatus.APPROVED);
        Map<Long, Long> likeCounts = likeCountsForReviews(reviews);
        Map<Long, List<MediaResponse>> mediaByReviewId = mediaForReviews(reviews);

        return reviews.stream()
                .map(r -> new ReviewResponse(
                        r.getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getUsername(),
                        r.getCreatedAt(),
                        likeCounts.getOrDefault(r.getId(), 0L),
                        r.getAdminReply(),
                        mediaByReviewId.getOrDefault(r.getId(), List.of())
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

    private MediaKind kindForContentType(String contentType) {
        if (contentType == null) {
            throw new UnsupportedMediaTypeException("null");
        }
        return switch (contentType) {
            case "image/jpeg", "image/png", "image/webp" -> MediaKind.IMAGE;
            case "video/mp4" -> MediaKind.VIDEO;
            default -> throw new UnsupportedMediaTypeException(contentType);
        };
    }

    private MediaResponse toMediaResponse(ReviewMedia media) {
        String base64 = Base64.getEncoder().encodeToString(media.getData());
        return new MediaResponse(
                media.getId(),
                media.getKind(),
                media.getContentType(),
                media.getSizeBytes(),
                media.getFilename(),
                media.getSortOrder(),
                base64
        );
    }

    private Map<Long, List<MediaResponse>> mediaForReviews(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = reviews.stream().map(Review::getId).toList();
        List<ReviewMedia> media = reviewMediaRepository.findAllByReview_IdIn(ids);

        return media.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getReview().getId(),
                        Collectors.mapping(this::toMediaResponse, Collectors.toList())
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                                .toList()
                ));
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
