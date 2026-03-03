package externalproxy.domain.dto;

import externalproxy.domain.enumeration.ReviewStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminReviewResponse {
    private Long id;
    private int rating;
    private String comment;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    private long likeCount;
    private ReviewStatus status;
    private String adminReply;
    private LocalDateTime approvedAt;
    private Long approvedByAdminId;
    private LocalDateTime repliedAt;
    private Long repliedByAdminId;
    private LocalDateTime deletedAt;
    private Long deletedByAdminId;
}

