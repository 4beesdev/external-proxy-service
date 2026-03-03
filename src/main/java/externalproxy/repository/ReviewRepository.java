package externalproxy.repository;

import externalproxy.domain.Review;
import externalproxy.domain.enumeration.ReviewStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    long countByIpHash(String ipHash);

    List<Review> findAllByStatusOrderByCreatedAtDesc(ReviewStatus status);

    @Query("select r from Review r where r.status <> externalproxy.domain.enumeration.ReviewStatus.DELETED order by r.createdAt desc")
    List<Review> findAllNotDeletedOrderByCreatedAtDesc();

    long countByStatus(ReviewStatus status);

    @Query("select avg(r.rating) from Review r where r.status = :status")
    Double getAverageRatingByStatus(@Param("status") ReviewStatus status);

    @Query("""
            select r.rating as rating, count(r) as cnt
            from Review r
            where r.status = :status
            group by r.rating
            """)
    List<RatingCount> countByRatingForStatus(@Param("status") ReviewStatus status);
}
