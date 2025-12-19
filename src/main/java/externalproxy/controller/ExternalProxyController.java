package externalproxy.controller;

import externalproxy.service.ExternalProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExternalProxyController {

    private final ExternalProxyService externalProxyService;

    @GetMapping("/api/statushistory/{id}")
    public ResponseEntity<?> getStatusHistory(@PathVariable("id") String id) {
        return externalProxyService.callExternalService(id);
    }
}
