package dev.group.cybershield.common.utils;

import dev.group.cybershield.common.constants.Constants;
import dev.group.cybershield.common.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.SecureRandomParameters;
import java.util.Arrays;
import java.util.Base64;

public class CommonUtils {

    @SneakyThrows
    public static String getToken(HttpServletRequest request) {
        if (request == null) {
            throw new Exception("Invalid request !");
        }

        String accessTokenKey = Constants.accessTokenNamePrefix + request.getServerName();
        String token = null;
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = request.getHeader("Authorization").substring(7);
        } else if (request.getCookies() != null && request.getCookies().length > 0) {
            token = Arrays.stream(request.getCookies())
                    .filter(cookie -> cookie != null && accessTokenKey.equalsIgnoreCase(cookie.getName())
                            && StringUtils.hasText(cookie.getValue()))
                    .map(Cookie::getValue)
                    .findFirst().orElse(null);
        }
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Token not found !");
        }
        return token;
    }

    public static String toCamelCase(String str){
        return str.substring(0,1).toUpperCase()+str.substring(1).toLowerCase();
    }

    public static String generateOpaqueToken(){
        byte[] bytes = new byte[32];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String urlDecode(String str){
        return URLDecoder.decode(str,StandardCharsets.UTF_8);
//        return new String(Base64.getUrlDecoder().decode(str.getBytes(StandardCharsets.UTF_8)));
    }

}
