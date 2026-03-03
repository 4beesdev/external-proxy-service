package externalproxy.domain.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewAverageResponse {
    private double averageRating;
    private long totalCount;
    private Map<Integer, Long> ratingCounts;
}

