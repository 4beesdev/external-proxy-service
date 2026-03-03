package externalproxy.repository;

import externalproxy.domain.ReviewMedia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {
    List<ReviewMedia> findAllByReview_IdIn(List<Long> reviewIds);
}

