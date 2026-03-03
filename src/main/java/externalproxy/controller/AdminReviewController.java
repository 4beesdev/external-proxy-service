package externalproxy.controller;

import externalproxy.domain.dto.AdminReplyRequest;
import externalproxy.domain.dto.AdminReviewResponse;
import externalproxy.domain.enumeration.ReviewStatus;
import externalproxy.service.AdminReviewService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping
    public List<AdminReviewResponse> list(Authentication authentication) {
        return adminReviewService.list(authentication);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(Authentication authentication, @PathVariable("id") long id) {
        adminReviewService.approve(authentication, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<Void> reply(Authentication authentication, @PathVariable("id") long id, @Valid @RequestBody AdminReplyRequest request) {
        adminReviewService.reply(authentication, id, request.getReply());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable("id") long id) {
        adminReviewService.softDelete(authentication, id);
        return ResponseEntity.noContent().build();
    }
}

