package dev.group.cybershield.security.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.group.cybershield.common.exception.BadRequestException;
import dev.group.cybershield.security.model.UserInfoModel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
public class JwtService {

    private static final String accessTokenSignKeyStr = "TestSecurityAppJwtKeyForJwtAccessTokenCreation";
    private static final String refreshTokenSignKeyStr = "TestSecurityAppJwtKeyForJwtRefreshTokenCreation";

    private static final SecretKey accessTokenSignKey = new SecretKeySpec(accessTokenSignKeyStr.getBytes(), "HmacSHA256");
    private static final SecretKey refreshTokenSignKey = new SecretKeySpec(refreshTokenSignKeyStr.getBytes(), "HmacSHA256");

    public String createJwtAccessToken(UserInfoModel userToBeLoggedIn) {
        Date issuedAt = new Date();
        Date expireAt = new Date(issuedAt.toInstant().plusSeconds(5 * 60).toEpochMilli());

        return Jwts.builder()
                .subject(userToBeLoggedIn.getEmail())
                .issuer("TestSecurityApp")
                .issuedAt(issuedAt)
                .expiration(expireAt)
                .claim("Role", userToBeLoggedIn.getRoles())
                .signWith(accessTokenSignKey)
                .compact();
    }

    public String createJwtRefreshToken(UserInfoModel userToBeLoggedIn) {
        Date issuedAt = new Date();
        Date expireAt = new Date(issuedAt.toInstant().plusSeconds(10 * 60).toEpochMilli());
        return Jwts.builder()
                .subject(userToBeLoggedIn.getEmail())
                .issuer("TestSecurityApp")
                .issuedAt(issuedAt)
                .expiration(expireAt)
                .claim("Role", userToBeLoggedIn.getRoles())
                .signWith(refreshTokenSignKey)
                .compact();
    }

    public boolean isValidJwtToken(String jwt, boolean isRefreshToken) {
        try {
            Jws<Claims> parsedJwt = Jwts.parser()
                    .verifyWith(isRefreshToken ? refreshTokenSignKey : accessTokenSignKey)
                    .build()
                    .parseSignedClaims(jwt);
            return true;
        } catch (ExpiredJwtException e) {
            System.out.println("JWT token expired ! = " + e.getMessage());
            throw new BadRequestException("JWT token expired !");
        } catch (SignatureException e) {
            System.out.println("JWT token is tampered ! = " + e.getMessage());
            throw new BadRequestException("JWT token is tampered !");
        } catch (Exception e) {
            System.out.println("exception occurred while validating jwt token = " + e.getMessage());
            throw new RuntimeException("exception occurred while validating jwt token");
        }
    }

    @SneakyThrows
    public <T> T extractPayloadFromJwt(String jwtToken, Class<T> type) {
        try {
            String[] jwtSplited = jwtToken.split("\\.");
            String payloadBase64 = jwtSplited[1];
            String payloadDecoded = new String(Base64.getUrlDecoder().decode(payloadBase64.getBytes()));
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(payloadDecoded, type);
        } catch (JsonProcessingException e) {
            log.error("Invalid jwt payload {}", e.getMessage());
            e.printStackTrace();
            throw new BadRequestException("Invalid jwt payload !");
        } catch (Exception e) {
            log.error("extractPayloadFromJwt_error {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @SneakyThrows
    public <T> T extractHeaderFromJwt(String jwtToken, Class<T> type) {
        try {
            String[] jwtSplited = jwtToken.split("\\.");
            String payloadBase64 = jwtSplited[0];
            String payloadDecoded = new String(Base64.getUrlDecoder().decode(payloadBase64.getBytes()));
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(payloadDecoded, type);
        } catch (JsonProcessingException e) {
            log.error("Invalid jwt header {}", e.getMessage());
            e.printStackTrace();
            throw new BadRequestException("Invalid jwt header !");
        } catch (Exception e) {
            log.error("extractHeaderFromJwt_error {}", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public String extractUsernameFromJwt(String jwtToken) {
        JsonNode payload = extractPayloadFromJwt(jwtToken, JsonNode.class);
        return payload.hasNonNull("sub") ? payload.get("sub").asText() : null;
    }

    public ZonedDateTime extractExpiryFromJwt(String jwtToken) {
        JsonNode payload = extractPayloadFromJwt(jwtToken, JsonNode.class);
        Instant expiry = payload.hasNonNull("exp") ?
                Instant.ofEpochSecond(payload.get("exp").asLong())
                : Instant.now().plusSeconds(60);
        return ZonedDateTime.ofInstant(expiry, ZoneId.systemDefault());
    }

    public List<String> extractRoleFromJwt(String jwtToken) {
        Map<String, Object> payload = extractPayloadFromJwt(jwtToken, Map.class);
        return payload.containsKey("Role") && payload.get("Role") instanceof List
                ? (List<String>) payload.get("Role") : new ArrayList<>();
    }

}
