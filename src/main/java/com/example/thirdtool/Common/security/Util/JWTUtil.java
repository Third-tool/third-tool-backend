package com.example.thirdtool.Common.security.Util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class JWTUtil {

    private static final SecretKey secretKey;
    private static final Long accessTokenExpiresIn;
    private static final Long refreshTokenExpiresIn;

    static  {
        String secretKeyString = "himynameiskimjihunmyyoutubechann";
        secretKey = new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());

        accessTokenExpiresIn = 3600L * 1000; // 1시간
        refreshTokenExpiresIn = 604800L * 1000; // 7일
    }

    // JWT 클레임 username 파싱
    public static String getUsername(String token) {
        return Jwts.parser()
                   .verifyWith(secretKey)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload()
                   .get("sub", String.class);
    }

    // JWT 클레임 role 파싱
    public static String getRole(String token) {
        return Jwts.parser()
                   .verifyWith(secretKey)
                   .build()
                   .parseSignedClaims(token)
                   .getPayload()
                   .get("role", String.class);
    }

    // JWT 유효 여부 (위조, 시간, Access/Refresh 여부)
    public static Boolean isValid(String token, Boolean isAccess) {
        try {
            log.debug("[JWT-VALIDATION] 토큰 검증 시작: {}", token);
            Claims claims = Jwts.parser()
                                .verifyWith(secretKey)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();

            String type = claims.get("type", String.class);
            log.debug("[JWT-VALIDATION] 추출된 type: {}", type);

            if (type == null) {
                log.warn("[JWT-VALIDATION] 토큰에 type 클레임이 없음");
                return false;
            }

            if (isAccess && !type.equals("access")) {
                log.warn("[JWT-VALIDATION] Access 토큰 기대했는데 Refresh 토큰이 들어옴");
                return false;
            }
            if (!isAccess && !type.equals("refresh")) {
                log.warn("[JWT-VALIDATION] Refresh 토큰 기대했는데 Access 토큰이 들어옴");
                return false;
            }

            log.debug("[JWT-VALIDATION] 토큰 검증 성공");
            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // JWT(Access/Refresh) 생성
    public static String createJWT(String username, String role, Boolean isAccess) {

        long now = System.currentTimeMillis();
        long expiry = isAccess ? accessTokenExpiresIn : refreshTokenExpiresIn;
        String type = isAccess ? "access" : "refresh";

        return Jwts.builder()
                   .claim("sub", username)
                   .claim("role", role)
                   .claim("type", type)
                   .issuedAt(new Date(now))
                   .expiration(new Date(now + expiry))
                   .signWith(secretKey)
                   .compact();
    }

}