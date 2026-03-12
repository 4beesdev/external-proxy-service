package externalproxy.repository;

import externalproxy.domain.ReviewLike;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    @Query("""
            select rl.review.id as reviewId, count(rl) as cnt
            from ReviewLike rl
            where rl.review.id in :reviewIds
            group by rl.review.id
            """)
    List<ReviewLikeCount> countByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    long countByReviewId(Long reviewId);

    Optional<ReviewLike> findByReviewIdAndIpHash(Long reviewId, String ipHash);

    @Query("select rl.review.id from ReviewLike rl where rl.ipHash = :ipHash")
    Set<Long> findReviewIdsByIpHash(@Param("ipHash") String ipHash);
}

