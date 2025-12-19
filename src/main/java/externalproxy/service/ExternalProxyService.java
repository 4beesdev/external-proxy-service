package externalproxy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class ExternalProxyService {

    private final WebClient webClient;

    @Value("${external.api.key}")
    private String externalApiKey;

    @Value("${api.external.url}")
    private String url;

    public ResponseEntity<?> callExternalService(String id) {
        try {
            return webClient
                    .get()
                    .uri(url+ id)
                    .header("x-api-key", externalApiKey)
                    .exchangeToMono(response -> response.toEntity(String.class))
                    .block();
        }catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("External service unavailable");
        }
    }
}
