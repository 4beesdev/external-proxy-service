package externalproxy.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponse {
    private Long id;
    private int rating;
    private String comment;
    private String username;
    private LocalDateTime createdAt;
    private long likeCount;
    private boolean hasLiked;
    private String adminReply;
    private LocalDateTime repliedAt;
    private List<MediaResponse> media;
}

