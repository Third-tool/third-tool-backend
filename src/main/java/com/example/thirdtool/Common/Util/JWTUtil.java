package com.example.thirdtool.Common.Util;


import com.example.thirdtool.Common.security.auth.jwt.JwtProperties;
import com.example.thirdtool.Common.security.auth.token.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
public class JWTUtil {

    private final SecretKey secretKey;
    private final JwtProperties properties;

    public JWTUtil(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = new SecretKeySpec(
                properties.secretKey().getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );
    }

    public String getUsername(String token) {
        return parseClaims(token).get("sub", String.class);
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isValid(String token, TokenType expectedType) {
        try {
            Claims claims = parseClaims(token);
            String type = claims.get("type", String.class);
            if (type == null) {
                log.warn("[JWT-VALIDATION] type claim 누락");
                return false;
            }
            if (!type.equals(expectedType.claim())) {
                log.warn("[JWT-VALIDATION] type 불일치 - 예상={}, 실제={}", expectedType.claim(), type);
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("[JWT-VALIDATION] 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public String createJWT(String username, String role, Duration ttl, TokenType type) {
        long now = System.currentTimeMillis();
        long expiry = ttl.toMillis();

        return Jwts.builder()
                   .claim("sub", username)
                   .claim("role", role)
                   .claim("type", type.claim())
                   .issuedAt(new Date(now))
                   .expiration(new Date(now + expiry))
                   .signWith(secretKey)
                   .compact();
    }

    public Duration accessTokenTtl() {
        return properties.accessTokenTtl();
    }

    public Duration refreshTokenTtl() {
        return properties.refreshTokenTtl();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                   .verifyWith(secretKey)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }
}
