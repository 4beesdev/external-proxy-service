package externalproxy.support;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IpHashService {

    private final byte[] secret;

    public IpHashService(@Value("${ip.hash.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Missing ip.hash.secret / IP_HASH_SECRET");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String hashIp(String ip) {
        if (ip == null || ip.isBlank()) {
            throw new IllegalArgumentException("Missing client IP");
        }
        byte[] hmac = hmacSha256(ip.trim().getBytes(StandardCharsets.UTF_8));
        return toHex(hmac);
    }

    private byte[] hmacSha256(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute HMAC-SHA256", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}

