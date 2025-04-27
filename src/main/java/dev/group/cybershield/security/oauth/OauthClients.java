package dev.group.cybershield.security.oauth;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Data
@ConfigurationProperties(prefix = "oauth")
public class OauthClients {

    private Map<String, ClientInfo> clients = new HashMap<>();

    @Data
    public static class ClientInfo {
        private String clientsId;
        private String clientSecret;
        private String grantType;
        private String redirectUrl;
        private String scope;
        private String authUrl;
        private String tokenUrl;
        private List<String> userInfoUrls;
    }

    @PostConstruct
    public void init() {
        clients.forEach((key, clientInfo) -> {
            if (clientInfo.getClientsId() != null) {
                clientInfo.setClientsId(clientInfo.getClientsId().trim());
            }
            if (clientInfo.getClientSecret() != null) {
                clientInfo.setClientSecret(clientInfo.getClientSecret().trim());
            }
            if (clientInfo.getGrantType() != null) {
                clientInfo.setGrantType(clientInfo.getGrantType().trim().toLowerCase());
            }
            if (clientInfo.getScope() != null) {
                clientInfo.setScope(clientInfo.getScope().trim().replace(",", " "));
            }
            if (clientInfo.getRedirectUrl() != null) {
                clientInfo.setRedirectUrl(trimAndRemoveTrailSlash(clientInfo.getRedirectUrl()));
            }
            if (clientInfo.getAuthUrl() != null) {
                clientInfo.setAuthUrl(trimAndRemoveTrailSlash(clientInfo.getAuthUrl()));
            }
            if (clientInfo.getTokenUrl() != null) {
                clientInfo.setTokenUrl(trimAndRemoveTrailSlash(clientInfo.getTokenUrl()));
            }
            if (clientInfo.getUserInfoUrls() != null) {
                List<String> userInfoUrls = clientInfo.getUserInfoUrls().stream().map(this::trimAndRemoveTrailSlash).toList();
                clientInfo.setUserInfoUrls(userInfoUrls);
            }
        });
    }

    private String trimAndRemoveTrailSlash(String url) {
        String finalUrl = url.trim();
        if (finalUrl.endsWith("/")) {
            finalUrl = finalUrl.substring(0, finalUrl.length() - 1);
        }
        return finalUrl;
    }
}
