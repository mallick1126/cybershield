package dev.group.cybershield.security.oauth.OauthModels;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OauthAuthorizeModel {
    String provider;
    String authorizeUrl;
    String state;
    String codeVerifier;
    String cachedKey;
}
