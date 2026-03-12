package externalproxy.service.auth;

import externalproxy.domain.Admin;
import externalproxy.utils.Utils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${token.secret.key}")  // tajni ključ u application.properties
    private String jwtSecretKey;

    @Value("${token.expiration}")  // trajanje tokena u milisekundama
    private Long jwtExpiration;

    // izvlacimo email iz tokena
    public String extractUserEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // generisanje tokena za korisnika
    public String generateToken(Admin admin) {
        return generateToken(new HashMap<>(), admin);
    }

    // provera da li je token validan
    public boolean isTokenValid(String token, Admin userAccount) {
        final String email = extractUserEmail(token);
        return (email.equals(userAccount.getEmail())) && !isTokenExpired(token);
    }


    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) { //izvlaci podatke iz tokena
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private String generateToken(Map<String, Object> extraClaims, Admin admin) { //kreiranje tokena
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(admin.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) { //parsira jwt token i vraca sve podatke iz tokena (claims)
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        token = token.trim();

        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() { //dekodira tajni ključ iz Base64 i pravi Key objekat za potpisivanje
        byte[] keyBytes = Utils.hexToBytes(jwtSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
