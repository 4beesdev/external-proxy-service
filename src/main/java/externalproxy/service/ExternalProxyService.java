package externalproxy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalProxyService {

    private final WebClient webClient;

    @Value("${external.api.key}")
    private String externalApiKey;

    @Value("${external.pudo.api.key}")
    private String externalPudoApiKey;

    @Value("${api.external.url}")
    private String url;

    @Value("${api.external.pudo.url}")
    private String pudoUrl;

    public ResponseEntity<String> getPudoLocations() {
        return proxyGetRequest(pudoUrl, externalPudoApiKey);
    }

    public ResponseEntity<?> callExternalService(String id) {
        return proxyGetRequest(url + id, externalApiKey);
    }

    private ResponseEntity<String> proxyGetRequest(String targetUrl, String apiKey) {
        try {
            return webClient
                    .get()
                    .uri(targetUrl)
                    .header("Accept", "application/json")
                    .header("X-API-Key", apiKey)
                    .exchangeToMono(response -> response.toEntity(String.class))
                    .block();
        } catch (Exception e) {
            log.error("Proxy GET failed url={} : {}", targetUrl, e.toString(), e);
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("External service unavailable");
        }
    }
}
