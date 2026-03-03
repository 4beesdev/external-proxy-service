package externalproxy.support;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    public String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) {
                return stripPort(stripQuotes(first));
            }
        }

        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isBlank()) {
            String ip = parseForwardedFor(forwarded);
            if (ip != null && !ip.isBlank()) {
                return stripPort(stripQuotes(ip.trim()));
            }
        }

        return Optional.ofNullable(request.getRemoteAddr()).orElse("0.0.0.0");
    }

    private String parseForwardedFor(String forwarded) {
        String firstEntry = forwarded.split(",")[0];
        for (String part : firstEntry.split(";")) {
            String p = part.trim();
            if (p.regionMatches(true, 0, "for=", 0, 4)) {
                return p.substring(4).trim();
            }
        }
        return null;
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String stripPort(String value) {
        if (value.startsWith("[") && value.contains("]")) {
            int end = value.indexOf(']');
            return value.substring(1, end);
        }
        int colon = value.indexOf(':');
        if (colon > 0 && value.indexOf(':', colon + 1) == -1) {
            return value.substring(0, colon);
        }
        return value;
    }
}

